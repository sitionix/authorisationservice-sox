package com.sitionix.athssox.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    SITE_USER(1L, "SITE USER"),
    SUPER_ADMIN(2L, "SUPER ADMIN"),
    ECOSYSTEM_OWNER(3L, "ECOSYSTEM OWNER"),
    SITE_ADMIN(4L, "SITE ADMIN");

    private final Long id;
    private final String description;

    public static UserRole fromId(final Long id) {
        return Stream.of(UserRole.values())
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No role found for id: " + id));
    }
}
