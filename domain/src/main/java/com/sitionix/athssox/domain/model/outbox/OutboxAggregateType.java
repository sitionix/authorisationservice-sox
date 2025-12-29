package com.sitionix.athssox.domain.model.outbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxAggregateType {
    USER(1L, "USER"),
    SESSION(2L, "SESSION");

    private final Long id;
    private final String description;
}
