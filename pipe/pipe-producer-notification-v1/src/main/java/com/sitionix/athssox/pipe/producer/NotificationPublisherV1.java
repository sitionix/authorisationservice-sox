package com.sitionix.athssox.pipe.producer;

import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.app_afesox.ntfssox.events.notifications.kafka.NotificationsV1Producer;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
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
    public Class<EmailVerifyPayload> payloadType() {
        return EmailVerifyPayload.class;
    }

    @Override
    public String eventType() {
        return NotificationTemplate.EMAIL_VERIFY.getDescription();
    }

    @Override
    public void publish(final EmailVerifyPayload payload,
                        final ForgeOutboxPublishMetadata metadata) {
        log.info("Publish notification event for type: {}", this.eventType());
        if (isNull(payload) || isNull(metadata)) {
            throw new IllegalArgumentException("Outbox payload and metadata are required");
        }

        final NotificationEnvelope envelope = this.mapper.asEnvelope(payload, metadata);

        final String key = Objects.requireNonNull(metadata.getIdempotencyId(), "idempotencyId is required").toString();
        this.producer.send(key, envelope);
        log.info("Notification event published type={} idempotencyId={}", this.eventType(), key);
    }

}
