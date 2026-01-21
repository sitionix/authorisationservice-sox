package com.sitionix.athssox.domain.model.emailverify;

import java.util.UUID;

public record EmailVerificationTokenIssue(UUID tokenId, String rawToken) {
}
