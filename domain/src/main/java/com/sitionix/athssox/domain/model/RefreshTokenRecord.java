package com.sitionix.athssox.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenRecord {

    private Long id;

    private String tokenHash;

    private AuthUser user;

    private DeviceSession session;

    private RefreshTokenStatus status;

    private Instant expiresAt;

    private Instant createdAt;

    private Instant updatedAt;

    private UUID rotatedFromTokenId;

    private Instant usedAt;

    private Instant revokedAt;

    private String revokedReason;
}
