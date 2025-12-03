package com.sitionix.athssox.domain;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    PENDING_EMAIL_VERIFY(1L, "PENDING EMAIL VERIFY"),
    ACTIVE(2L, "ACTIVE"),
    INACTIVE(3L, "INACTIVE"),
    BANNED(4L, "BANNED");

    private final Long id;
    private final String description;

    public static UserStatus fromId(final Long id) {
        return Stream.of(UserStatus.values())
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No role found for id: " + id));
    }
}
