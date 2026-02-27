package com.sitionix.athssox.domain.repository;

import com.sitionix.forge.outbox.core.model.OutboxRecord;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository {

    void create(OutboxRecord outboxRecord);

    List<OutboxRecord> claimPendingEvents(List<String> eventStatuses,
                                          List<String> eventTypes,
                                          int batchSize,
                                          LocalDateTime now);

    void markSent(Long outboxEventId);

    void markFailed(Long outboxEventId,
                    String errorMessage,
                    Duration retryDelay,
                    int maxRetries);

    int deleteSentBefore(LocalDateTime cutoff);
}
