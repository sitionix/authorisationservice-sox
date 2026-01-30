package com.sitionix.athssox.domain.model.emailverify;

import java.time.Instant;
import java.util.UUID;

public record EmailVerificationLinkIssue(UUID tokenId, UUID siteId, String token, Instant expiresAt) {
}
