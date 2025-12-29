package com.sitionix.athssox.domain.model.outbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutboxEventType {
    EMAIL_VERIFY(1L, "EMAIL_VERIFY"),
    PASSWORD_RESET(2L, "PASSWORD_RESET");

    private final Long id;
    private final String description;
}
