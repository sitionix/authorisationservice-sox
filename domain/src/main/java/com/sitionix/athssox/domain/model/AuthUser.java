package com.sitionix.athssox.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthUser {

    private Long id;

    private String email;

    private String passwordHash;

    private UserStatus status;

    private UserRole role;

    private UUID siteId;
}
