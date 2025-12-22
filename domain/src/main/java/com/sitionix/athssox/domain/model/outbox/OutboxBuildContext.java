package com.sitionix.athssox.domain.model.outbox;


import java.time.Instant;
import java.util.UUID;

public record OutboxBuildContext(
        Long userId,
        UUID siteId,
        String email,
        String traceId,
        String userAgent,
        Instant requestedAt
) { }
