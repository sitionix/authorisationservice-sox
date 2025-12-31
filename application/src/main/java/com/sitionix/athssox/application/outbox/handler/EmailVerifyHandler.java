package com.sitionix.athssox.application.outbox.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.athssox.domain.event.EventHandler;
import com.sitionix.athssox.domain.exception.OutboxPayloadParseException;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import com.sitionix.athssox.domain.model.outbox.payload.handler.EventTypeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("emailVerifyHandler")
@RequiredArgsConstructor
public class EmailVerifyHandler implements EventTypeHandler {

    private final ObjectMapper objectMapper;

    private final EventHandler<EmailVerifyPayload> eventHandler;

    @Override
    public <P> void doHandle(final OutboxEvent<P> event) {
        final Event<EmailVerifyPayload> payloadEvent = Event.create(event);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P> P getPayload(final String payload) {
        try {
            return (P) this.objectMapper.readValue(payload, EmailVerifyPayload.class);
        } catch (final JsonProcessingException e) {
            throw new OutboxPayloadParseException("Payload cannot be parsed into EmailVerifyPayload due to error: " + e);
        }
    }
}
