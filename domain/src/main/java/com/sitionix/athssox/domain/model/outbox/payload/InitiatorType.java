package com.sitionix.athssox.domain.model.outbox.payload;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

@RequiredArgsConstructor
@Getter
public enum InitiatorType {
    USER(1L, "USER"),
    SYSTEM(2L, "SYSTEM"),
    SCHEDULER(3L, "SCHEDULER");

    private final Long id;

    private final String description;

    public static InitiatorType fromId(final Long id) {
        for (InitiatorType value : InitiatorType.values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No initiator type found for id: " + id);
    }
}
