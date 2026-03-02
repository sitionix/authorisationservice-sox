package com.sitionix.athssox.pipe.producer;

import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.app_afesox.ntfssox.events.notifications.kafka.NotificationsV1Producer;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.pipe.producer.mapper.NotificationEventMapper;
import com.sitionix.forge.outbox.core.model.Event;
import com.sitionix.forge.outbox.core.port.ForgeOutboxEventPublisher;
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
    public void publish(final Event<EmailVerifyPayload> event) {
        log.info("Publish notification event");
        if (isNull(event) || isNull(event.getPayload())) {
            throw new IllegalArgumentException("Outbox event and payload are required");
        }

        final String eventType = Objects.requireNonNull(event.getEventType(), "eventType is required");
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        final String key = Objects.requireNonNull(event.getIdempotencyId(), "idempotencyId is required").toString();
        Objects.requireNonNull(event.getCreatedAt(), "createdAt is required");

        final NotificationEnvelope envelope = this.mapper.asEnvelope(event);

        this.producer.send(key, envelope);
        log.info("Notification event published type={} idempotencyId={}", eventType, key);
    }

}
