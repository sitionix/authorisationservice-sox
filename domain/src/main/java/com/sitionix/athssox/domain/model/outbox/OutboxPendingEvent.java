package com.sitionix.athssox.domain.model.outbox;

import java.time.LocalDateTime;

public record OutboxPendingEvent(
        Long id,
        Long aggregateId,
        OutboxEventType eventType,
        String payload,
        LocalDateTime createdAt
) {
}
