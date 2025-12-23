package com.sitionix.athssox.domain.model.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent<P> {

    private OutboxAggregateType aggregateType;
    private Long aggregateId;
    private OutboxEventType eventType;
    private OutboxStatus status;
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private P payload;
    private String lastError;
}
