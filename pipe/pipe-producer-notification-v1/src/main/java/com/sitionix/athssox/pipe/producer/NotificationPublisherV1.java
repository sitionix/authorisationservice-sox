package com.sitionix.athssox.pipe.producer;

import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.app_afesox.ntfssox.events.notifications.kafka.NotificationsV1Producer;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.pipe.producer.mapper.NotificationEventMapper;
import com.sitionix.forge.outbox.core.port.ForgeOutboxEventPublisher;
import com.sitionix.forge.outbox.core.port.ForgeOutboxPublishMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static java.util.Objects.isNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisherV1 implements ForgeOutboxEventPublisher<EmailVerifyPayload> {

    private final NotificationsV1Producer producer;

    private final NotificationEventMapper mapper;

    @Override
    public void publish(final EmailVerifyPayload payload,
                        final ForgeOutboxPublishMetadata metadata) {
        log.info("Publish notification event");
        if (isNull(payload) || isNull(metadata)) {
            throw new IllegalArgumentException("Outbox payload and metadata are required");
        }

        final String eventType = Objects.requireNonNull(metadata.getEventType(), "eventType is required");
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        final String key = Objects.requireNonNull(metadata.getIdempotencyId(), "idempotencyId is required").toString();
        Objects.requireNonNull(metadata.getCreatedAt(), "createdAt is required");

        final NotificationEnvelope envelope = this.mapper.asEnvelope(payload, metadata);

        this.producer.send(key, envelope);
        log.info("Notification event published type={} idempotencyId={}", eventType, key);
    }

}
