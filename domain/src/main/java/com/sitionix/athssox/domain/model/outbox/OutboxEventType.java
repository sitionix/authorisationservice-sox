package com.sitionix.athssox.domain.model.outbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum OutboxEventType {

    EMAIL_VERIFY(1L, "EMAIL_VERIFY");

    private final Long id;
    private final String description;

    public static OutboxEventType fromId(final Long id) {
        return Stream.of(OutboxEventType.values())
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No event type found for id: " + id));
    }
}
