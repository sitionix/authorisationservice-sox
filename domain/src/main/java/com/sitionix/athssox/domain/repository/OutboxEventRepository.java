package com.sitionix.athssox.domain.repository;

import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxPendingEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository {

    void create(final OutboxEvent<?> outboxEvent);

    List<OutboxPendingEvent> claimPendingEvents(List<OutboxEventType> eventTypes,
                                                int batchSize,
                                                LocalDateTime now);

    void markSent(Long outboxEventId);

    void markFailed(Long outboxEventId,
                    String errorMessage,
                    Duration retryDelay,
                    int maxRetries);
}
