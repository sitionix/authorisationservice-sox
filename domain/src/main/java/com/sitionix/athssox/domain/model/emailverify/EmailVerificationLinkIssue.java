package com.sitionix.athssox.domain.model.emailverify;

import java.time.Instant;
import java.util.UUID;

public record EmailVerificationLinkIssue(UUID tokenId, String verifyUrl, Instant expiresAt) {
}
