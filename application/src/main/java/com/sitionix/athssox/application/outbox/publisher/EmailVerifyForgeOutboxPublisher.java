package com.sitionix.athssox.application.outbox.publisher;

import com.sitionix.athssox.domain.event.EventHandler;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import com.sitionix.forge.outbox.core.model.OutboxRecord;
import com.sitionix.forge.outbox.core.port.OutboxPayloadCodec;
import com.sitionix.forge.outbox.core.service.TypedOutboxPublisher;
import org.springframework.stereotype.Component;

@Component
public class EmailVerifyForgeOutboxPublisher extends TypedOutboxPublisher<EmailVerifyPayload> {

    private final EventHandler<EmailVerifyPayload> eventHandler;

    public EmailVerifyForgeOutboxPublisher(final OutboxPayloadCodec outboxPayloadCodec,
                                           final EventHandler<EmailVerifyPayload> eventHandler) {
        super(outboxPayloadCodec, OutboxEventType.EMAIL_VERIFY.getDescription(), EmailVerifyPayload.class);
        this.eventHandler = eventHandler;
    }

    @Override
    protected void publishTyped(final OutboxRecord record, final EmailVerifyPayload payload) {
        final OutboxEvent<EmailVerifyPayload> outboxEvent = OutboxEvent.<EmailVerifyPayload>builder()
                .id(Long.valueOf(record.getId()))
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .payload(payload)
                .createdAt(record.getCreatedAt())
                .build();

        this.eventHandler.publish(Event.create(outboxEvent));
    }
}
