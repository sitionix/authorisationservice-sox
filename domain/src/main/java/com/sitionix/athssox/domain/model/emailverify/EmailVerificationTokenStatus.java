package com.sitionix.athssox.domain.model.emailverify;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum EmailVerificationTokenStatus {
    ACTIVE(1L, "ACTIVE"),
    USED(2L, "USED"),
    REVOKED(3L, "REVOKED");

    private final Long id;
    private final String description;

    public static EmailVerificationTokenStatus fromId(final Long id) {
        return Stream.of(EmailVerificationTokenStatus.values())
                .filter(status -> status.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No email verification token status found for id: " + id));
    }
}
