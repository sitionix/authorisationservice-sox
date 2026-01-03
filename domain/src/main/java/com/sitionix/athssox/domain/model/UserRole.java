package com.sitionix.athssox.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
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

    public boolean isSiteScoped() {
        return this == SITE_USER || this == SITE_ADMIN;
    }

    public boolean isGlobalScoped() {
        return this == SUPER_ADMIN || this == ECOSYSTEM_OWNER;
    }

    public static List<Long> siteScopedIds() {
        return List.of(SITE_USER.getId(),
                SITE_ADMIN.getId());
    }

    public static List<Long> globalScopedIds() {
        return List.of(SUPER_ADMIN.getId(),
                ECOSYSTEM_OWNER.getId());
    }

    public static UserRole fromId(final Long id) {
        return Stream.of(UserRole.values())
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No role found for id: " + id));
    }
}
