package com.sitionix.athssox.domain.model.outbox;

import com.sitionix.athssox.domain.model.outbox.payload.handler.EventTypeHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum OutboxEventType implements EventTypeHandler<Object> {

    EMAIL_VERIFY(1L, "EMAIL_VERIFY", "emailVerifyHandler");

    private final Long id;
    private final String description;
    private final String serviceName;

    @Setter
    private EventTypeHandler<Object> handler;

    @Override
    public void doHandle(final OutboxEvent<Object> event) {
        this.handler.doHandle(event);
    }

    @Override
    public Object getPayload(final String payload) {
        return this.handler.getPayload(payload);
    }

    public static OutboxEventType fromId(final Long id) {
        return Stream.of(OutboxEventType.values())
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No event type found for id: " + id));
    }
}
