package com.sitionix.athssox.domain.model.outbox;

import com.sitionix.athssox.domain.model.outbox.payload.InitiatorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent<P> {

    private Long id;
    private Long aggregateId;
    private String initiatorId;
    private OutboxAggregateType aggregateType;
    private OutboxEventType eventType;
    private OutboxStatus status;
    private InitiatorType initiatorType;
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private P payload;
    private String lastError;
    private Instant createdAt;
}
