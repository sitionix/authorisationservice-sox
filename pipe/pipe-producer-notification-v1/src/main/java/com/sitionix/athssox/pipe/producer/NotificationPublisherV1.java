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
        return EmailVerifyPayload.OUTBOX_EVENT_TYPE;
    }

    @Override
    public void publish(final EmailVerifyPayload payload,
                        final ForgeOutboxPublishMetadata metadata) {
        log.info("Publish notification event for type: {}", this.eventType());
        if (isNull(payload) || isNull(metadata)) {
            return;
        }

        final NotificationEnvelope envelope = this.mapper.asEnvelope(payload, metadata);

        this.producer.send(metadata.getIdempotencyId().toString(), envelope);
        log.info("Notification event published: {}", envelope);
    }

}
