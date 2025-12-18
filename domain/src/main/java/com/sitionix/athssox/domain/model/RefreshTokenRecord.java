package com.sitionix.athssox.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenRecord {

    private String tokenHash;

    private Long userId;

    private Instant expiresAt;
}
