package com.sitionix.athssox.application.outbox;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.athssox.domain.config.OutboxWorkerConfig;
import com.sitionix.athssox.domain.event.EventHandler;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxPendingEvent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

//@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxWorkerConfig config;
    private final EventHandler<EmailVerifyPayload> emailVerifyPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "#{@outboxWorkerConfig.pollDelayMs}")
    public void dispatchPendingEvents() {
        final List<OutboxPendingEvent> events = this.outboxEventRepository.claimPendingEvents(
                this.supportedEventTypes(),
                this.config.getBatchSize(),
                LocalDateTime.now());
        if (events.isEmpty()) {
            return;
        }

        for (final OutboxPendingEvent event : events) {
            this.handleEvent(event);
        }
    }

    private void handleEvent(final OutboxPendingEvent event) {
        try {
            this.publishEvent(event);
            this.outboxEventRepository.markSent(event.id());
        } catch (final Exception exception) {
//            log.warn("Failed to publish outbox event id={}", event.id(), exception);
            this.outboxEventRepository.markFailed(
                    event.id(),
                    this.formatErrorMessage(exception),
                    Duration.ofSeconds(this.config.getRetryDelaySeconds()),
                    this.config.getMaxRetries());
        }
    }

    private List<OutboxEventType> supportedEventTypes() {
        return List.of(OutboxEventType.EMAIL_VERIFY);
    }

    private void publishEvent(final OutboxPendingEvent event) {
        if (event.eventType() == null) {
            throw new IllegalStateException("Outbox event type is missing");
        }

        switch (event.eventType()) {
            case EMAIL_VERIFY -> this.publishEmailVerifyEvent(event);
            default -> throw new IllegalStateException("No publisher configured for eventType=" + event.eventType());
        }
    }

    private void publishEmailVerifyEvent(final OutboxPendingEvent event) {
        final EmailVerifyPayload payload = this.parseEmailVerifyPayload(event.payload(), event.aggregateId());
        if (payload == null) {
            throw new IllegalStateException("Outbox payload is missing");
        }

        final Event<EmailVerifyPayload> domainEvent = this.buildEmailVerifyEvent(payload, event);
        this.emailVerifyPublisher.publish(domainEvent);
    }

    private EmailVerifyPayload parseEmailVerifyPayload(final String payloadJson,
                                                       final Long aggregateId) {
        if (payloadJson == null) {
            return null;
        }

        final EmailVerifyPayloadDto dto;
        try {
            dto = this.objectMapper
                    .readerFor(EmailVerifyPayloadDto.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(payloadJson);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to parse outbox payload", exception);
        }
        final EmailVerifyPayloadDto.Meta meta = dto.meta;

        return EmailVerifyPayload.builder()
                .id(aggregateId)
                .delivery(this.toDelivery(dto.delivery))
                .template(this.toTemplate(dto.template))
                .params(this.toParams(dto.params))
                .meta(this.toMeta(meta))
                .build();
    }

    private EmailVerifyPayload.Delivery toDelivery(final EmailVerifyPayloadDto.Delivery delivery) {
        if (delivery == null) {
            return null;
        }
        return EmailVerifyPayload.Delivery.builder()
                .channel(this.toChannel(delivery.channel))
                .to(delivery.to)
                .build();
    }

    private VerifyChannel toChannel(final String channel) {
        if (channel == null) {
            return null;
        }
        return VerifyChannel.valueOf(channel);
    }

    private NotificationTemplate toTemplate(final String template) {
        if (template == null) {
            return null;
        }
        return NotificationTemplate.valueOf(template);
    }

    private EmailVerifyPayload.Params toParams(final EmailVerifyPayloadDto.Params params) {
        if (params == null) {
            return null;
        }
        return EmailVerifyPayload.Params.builder()
                .verifyUrl(params.verifyUrl)
                .build();
    }

    private EmailVerifyPayload.Meta toMeta(final EmailVerifyPayloadDto.Meta meta) {
        if (meta == null) {
            return null;
        }
        return EmailVerifyPayload.Meta.builder()
                .user(meta.user)
                .userId(meta.userId)
                .siteId(this.toSiteId(meta.siteId))
                .traceId(meta.traceId)
                .requestedAt(this.toRequestedAt(meta.requestedAt))
                .build();
    }

    private UUID toSiteId(final String siteId) {
        if (siteId == null) {
            return null;
        }
        return UUID.fromString(siteId);
    }

    private Instant toRequestedAt(final String requestedAt) {
        if (requestedAt == null) {
            return null;
        }
        return Instant.parse(requestedAt);
    }

    private Event<EmailVerifyPayload> buildEmailVerifyEvent(final EmailVerifyPayload payload,
                                                            final OutboxPendingEvent event) {
        return new Event<>(
                payload,
                this.resolveUser(payload),
                this.resolveEventType(event),
                this.resolveCreatedAt(event.createdAt()));
    }

    private String resolveUser(final EmailVerifyPayload payload) {
        if (payload.getMeta() != null && payload.getMeta().getUser() != null) {
            return payload.getMeta().getUser();
        }
        if (payload.getDelivery() != null) {
            return payload.getDelivery().getTo();
        }
        return null;
    }

    private String resolveEventType(final OutboxPendingEvent event) {
        if (event.eventType() == null) {
            return null;
        }
        return event.eventType().getDescription();
    }

    private Instant resolveCreatedAt(final LocalDateTime createdAt) {
        if (createdAt == null) {
            return Instant.now();
        }
        return createdAt.atZone(ZoneOffset.UTC).toInstant();
    }

    private String formatErrorMessage(final Exception exception) {
        final String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    static class EmailVerifyPayloadDto {
        public Delivery delivery;
        public String template;
        public Params params;
        public Meta meta;

        static class Delivery {
            public String channel;
            public String to;
        }

        static class Params {
            public String verifyUrl;
        }

        static class Meta {
            public String user;
            public Long userId;
            public String siteId;
            public String traceId;
            public String requestedAt;
        }
    }
}
