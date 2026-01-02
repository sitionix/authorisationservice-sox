package com.sitionix.athssox.domain.model.outbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum OutboxStatus {
    PENDING(1L, "PENDING"),
    IN_PROGRESS(2L, "IN_PROGRESS"),
    SENT(3L, "SENT"),
    FAILED(4L, "FAILED"),
    DEAD(5L, "DEAD");

    private final Long id;
    private final String description;

    public static OutboxStatus fromId(final Long id) {
        return Stream.of(OutboxStatus.values())
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No event status found for id: " + id));
    }
}
