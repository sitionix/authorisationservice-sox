package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import com.sitionix.athssox.postgresql.entity.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.OutboxStatusEntity;
import com.sitionix.athssox.postgresql.jpa.OutboxEventJpaRepository;
import com.sitionix.athssox.postgresql.jpa.OutboxInitiatorTypeJpaRepository;
import com.sitionix.athssox.postgresql.jpa.OutboxStatusJpaRepository;
import com.sitionix.athssox.postgresql.mapper.outbox.OutboxInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private static final long DEFAULT_INITIATOR_TYPE_ID = 2L;

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final OutboxStatusJpaRepository outboxStatusJpaRepository;
    private final OutboxInitiatorTypeJpaRepository outboxInitiatorTypeJpaRepository;
    private final OutboxInfraMapper outboxInfraMapper;

    @Override
    @Transactional
    public void create(final OutboxEvent<?> outboxEventCreate) {
        final OutboxEventEntity entity = this.outboxInfraMapper.toEntity(outboxEventCreate);
        if (entity.getInitiatorType() == null) {
            entity.setInitiatorType(this.outboxInitiatorTypeJpaRepository.getReferenceById(DEFAULT_INITIATOR_TYPE_ID));
        }
        this.outboxEventJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public List<OutboxEvent<Object>> claimPendingEvents(final List<String> eventStatuses,
                                                        final List<String> eventTypes,
                                                       final int batchSize,
                                                       final LocalDateTime now) {
        if (eventTypes == null || eventTypes.isEmpty()) {
            return List.of();
        }

        final Pageable pageable = PageRequest.of(0, batchSize);
        final List<OutboxEventEntity> events = this.outboxEventJpaRepository.findPendingForUpdate(
                eventStatuses,
                eventTypes,
                now,
                pageable);

        if (events.isEmpty()) {
            return List.of();
        }

        final OutboxStatusEntity inProgress = this.outboxStatusJpaRepository
                .getReferenceById(OutboxStatus.IN_PROGRESS.getId());

        for (final OutboxEventEntity event : events) {
            event.setStatus(inProgress);
        }

        return this.outboxEventJpaRepository.saveAll(events).stream()
                .map(this.outboxInfraMapper::toOutboxEvent)
                .toList();
    }

    @Override
    @Transactional
    public void markSent(final Long outboxEventId) {
        this.outboxEventJpaRepository.findById(outboxEventId)
                .ifPresent(event -> {
                    event.setStatus(this.outboxStatusJpaRepository.getReferenceById(OutboxStatus.SENT.getId()));
                    event.setLastError(null);
                    this.outboxEventJpaRepository.save(event);
                });
    }

    @Override
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
