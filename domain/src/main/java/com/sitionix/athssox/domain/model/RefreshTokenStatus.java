package com.sitionix.athssox.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum RefreshTokenStatus {
    ACTIVE(1L, "ACTIVE"),
    REVOKED(2L, "REVOKED");

    private final Long id;
    private final String description;

    public static RefreshTokenStatus fromId(final Long id) {
        return Stream.of(RefreshTokenStatus.values())
                .filter(status -> status.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No refresh token status found for id: " + id));
    }
}
