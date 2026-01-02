package com.sitionix.athssox.domain.model.emailverify;

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
public class EmailVerificationTokenRecord {

    private UUID id;

    private Long userId;

    private UUID siteId;

    private String tokenHash;

    private EmailVerificationTokenStatus status;

    private Instant expiresAt;

    private Instant usedAt;
}
