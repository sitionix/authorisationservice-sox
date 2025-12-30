package com.sitionix.athssox.domain.model.outbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum OutboxAggregateType {
    USER(1L, "USER"),
    SESSION(2L, "SESSION");

    private final Long id;
    private final String description;

    public static OutboxAggregateType fromId(final Long id) {
        return Stream.of(OutboxAggregateType.values())
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No event aggregate type found for id: " + id));
    }
}
