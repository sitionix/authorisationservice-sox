package com.sitionix.athssox.application.outbox.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.athssox.domain.event.EventHandler;
import com.sitionix.athssox.domain.exception.OutboxPayloadParseException;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import com.sitionix.forge.outbox.core.model.OutboxRecord;
import com.sitionix.forge.outbox.core.port.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class EmailVerifyForgeOutboxPublisher implements OutboxPublisher {

    private final ObjectMapper objectMapper;
    private final EventHandler<EmailVerifyPayload> eventHandler;

    @Override
    public Set<String> supportedEventTypes() {
        return Set.of(OutboxEventType.EMAIL_VERIFY.getDescription());
    }

    @Override
    public void publish(final OutboxRecord record) {
        final OutboxEventType outboxEventType = this.mapEventType(record.getEventType());
        final EmailVerifyPayload payload = this.parsePayload(record.getPayload());

        final OutboxEvent<EmailVerifyPayload> outboxEvent = OutboxEvent.<EmailVerifyPayload>builder()
                .id(Long.valueOf(record.getId()))
                .eventType(outboxEventType)
                .payload(payload)
                .createdAt(record.getCreatedAt())
                .build();

        this.eventHandler.publish(Event.create(outboxEvent));
    }

    private OutboxEventType mapEventType(final String eventType) {
        return Arrays.stream(OutboxEventType.values())
                .filter(value -> value.getDescription().equals(eventType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported event type: " + eventType));
    }

    private EmailVerifyPayload parsePayload(final String payload) {
        try {
            return this.objectMapper.readValue(payload, EmailVerifyPayload.class);
        } catch (final JsonProcessingException exception) {
            throw new OutboxPayloadParseException("Payload cannot be parsed into EmailVerifyPayload due to error: " + exception);
        }
    }
}
