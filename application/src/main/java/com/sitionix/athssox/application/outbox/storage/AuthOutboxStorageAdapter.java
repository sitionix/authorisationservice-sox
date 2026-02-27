package com.sitionix.athssox.application.outbox.storage;

import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import com.sitionix.forge.outbox.core.model.OutboxRecord;
import com.sitionix.forge.outbox.core.model.OutboxStatus;
import com.sitionix.forge.outbox.core.port.OutboxStorage;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public class AuthOutboxStorageAdapter implements OutboxStorage {

    private final OutboxEventRepository outboxEventRepository;

    public AuthOutboxStorageAdapter(final OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    public void enqueue(final OutboxRecord record) {
        this.outboxEventRepository.create(record);
    }

    @Override
    public List<OutboxRecord> claimPendingEvents(final Set<OutboxStatus> eventStatuses,
                                                 final Set<String> eventTypes,
                                                 final int batchSize,
                                                 final Instant now,
                                                 final boolean lockEnabled,
                                                 final Duration lockLease) {
        final List<String> statuses = eventStatuses.stream()
                .map(Enum::name)
                .toList();

        return this.outboxEventRepository.claimPendingEvents(
                        statuses,
                        List.copyOf(eventTypes),
                        batchSize,
                        java.time.LocalDateTime.ofInstant(now, java.time.ZoneOffset.UTC));
    }

    @Override
    public void markSent(final String outboxEventId) {
        this.outboxEventRepository.markSent(Long.valueOf(outboxEventId));
    }

    @Override
    public void markFailed(final String outboxEventId,
                           final String errorMessage,
                           final Duration retryDelay,
                           final int maxRetries,
                           final Instant now) {
        this.outboxEventRepository.markFailed(
                Long.valueOf(outboxEventId),
                errorMessage,
                retryDelay,
                maxRetries);
    }
}
