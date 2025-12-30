package com.sitionix.athssox.worker;

import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.postgresql.entity.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.OutboxStatusEntity;
import com.sitionix.athssox.postgresql.jpa.OutboxEventJpaRepository;
import com.sitionix.athssox.postgresql.jpa.OutboxStatusJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailVerifyOutboxEventService {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final OutboxStatusJpaRepository outboxStatusJpaRepository;

    @Transactional
    public List<OutboxEventEntity> claimPendingEvents(final int batchSize) {
        final Pageable pageable = PageRequest.of(0, batchSize);
        final List<OutboxEventEntity> events = this.outboxEventJpaRepository.findPendingForUpdate(
                List.of(
                        OutboxStatus.PENDING.getDescription(),
                        OutboxStatus.FAILED.getDescription()),
                OutboxEventType.EMAIL_VERIFY.getDescription(),
                LocalDateTime.now(),
                pageable);
        if (events.isEmpty()) {
            return events;
        }

        final OutboxStatusEntity inProgress = this.outboxStatusJpaRepository
                .getReferenceById(OutboxStatus.IN_PROGRESS.getId());
        for (final OutboxEventEntity event : events) {
            event.setStatus(inProgress);
        }

        return this.outboxEventJpaRepository.saveAll(events);
    }

    @Transactional
    public void markSent(final Long outboxEventId) {
        this.outboxEventJpaRepository.findById(outboxEventId)
                .ifPresent(event -> {
                    event.setStatus(this.outboxStatusJpaRepository.getReferenceById(OutboxStatus.SENT.getId()));
                    event.setLastError(null);
                    this.outboxEventJpaRepository.save(event);
                });
    }

    @Transactional
    public void markFailed(final Long outboxEventId,
                           final String errorMessage,
                           final Duration retryDelay,
                           final int maxRetries) {
        this.outboxEventJpaRepository.findById(outboxEventId)
                .ifPresent(event -> {
                    final int retryCount = event.getRetryCount() + 1;
                    event.setRetryCount(retryCount);
                    event.setLastError(errorMessage);
                    event.setNextRetryAt(LocalDateTime.now().plus(retryDelay));

                    final OutboxStatus status = retryCount >= maxRetries
                            ? OutboxStatus.DEAD
                            : OutboxStatus.FAILED;
                    event.setStatus(this.outboxStatusJpaRepository.getReferenceById(status.getId()));

                    this.outboxEventJpaRepository.save(event);
                });
    }
}
