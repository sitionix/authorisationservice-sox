package com.sitionix.athssox.domain.model.outbox;

import com.sitionix.athssox.domain.model.outbox.payload.handler.EventTypeHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum OutboxEventType implements EventTypeHandler {
    EMAIL_VERIFY(1L, "EMAIL_VERIFY", "emailVerifyHandler"),
    PASSWORD_RESET(2L, "PASSWORD_RESET", "passwordResetHandler");

    private final Long id;
    private final String description;
    private final String serviceName;

    @Setter
    private EventTypeHandler handler;

    @Override
    public <T> T getPayload(final String payload) {
        return handler.getPayload(payload);
    }

    public static OutboxEventType fromId(final Long id) {
        return Stream.of(OutboxEventType.values())
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No event type found for id: " + id));
    }
}
