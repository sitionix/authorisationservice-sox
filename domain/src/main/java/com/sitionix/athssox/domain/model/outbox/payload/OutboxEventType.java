package com.sitionix.athssox.domain.model.outbox.payload;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxEventType {
    EMAIL_VERIFY("EMAIL_VERIFY");

    private final String description;
}
