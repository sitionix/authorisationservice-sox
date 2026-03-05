package com.sitionix.athssox.pipe.producer;

import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.app_afesox.ntfssox.events.notifications.kafka.NotificationsV1Producer;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.pipe.producer.mapper.NotificationEventMapper;
import com.sitionix.forge.outbox.core.model.Event;
import com.sitionix.forge.outbox.core.port.ForgeTypedOutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisherV1 extends ForgeTypedOutboxEventPublisher<EmailVerifyPayload> {

    private final NotificationsV1Producer producer;

    private final NotificationEventMapper mapper;

    @Override
    protected Class<EmailVerifyPayload> payloadClass() {
        return EmailVerifyPayload.class;
    }

    @Override
    protected void publish(final Event<EmailVerifyPayload> event) {
        log.info("Publish notification event");
        final String key = event.getIdempotencyId().toString();
        final NotificationEnvelope envelope = this.mapper.asEnvelope(event);
        this.producer.send(key, envelope);
        log.info("Notification event published type={} idempotencyId={}", event.getEventType(), key);
    }

}
