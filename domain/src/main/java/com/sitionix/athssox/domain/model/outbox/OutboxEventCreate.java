package com.sitionix.athssox.domain.model.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEventCreate {

    private OutboxAggregateType aggregateType;
    private UUID aggregateId;
    private OutboxEventType eventType;
    private OutboxStatus status;
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private String payload;
    private String lastError;
}
