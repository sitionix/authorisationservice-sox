package com.sitionix.athssox.domain.model.emailverify;

import java.time.Instant;
import java.util.UUID;

public record EmailVerifyPayloadContext(
        Long userId,
        UUID siteId,
        String email,
        String traceId,
        String userAgent,
        Instant requestedAt
) { }
