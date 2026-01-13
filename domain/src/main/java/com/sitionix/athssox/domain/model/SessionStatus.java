package com.sitionix.athssox.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum SessionStatus {
    ACTIVE(1L, "ACTIVE"),
    SUSPICIOUS(2L, "SUSPICIOUS"),
    REVOKED_BY_USER(3L, "REVOKED BY USER"),
    REVOKED_BY_ADMIN(4L, "REVOKED BY ADMIN");

    private final Long id;
    private final String description;

    public static SessionStatus fromId(final Long id) {
        return Stream.of(SessionStatus.values())
                .filter(status -> status.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No session status found for id: " + id));
    }
}
