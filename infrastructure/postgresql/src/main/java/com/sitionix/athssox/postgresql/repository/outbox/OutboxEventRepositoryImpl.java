package com.sitionix.athssox.postgresql.repository.outbox;

import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.model.outbox.payload.InitiatorType;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxAggregateTypeEntity;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventTypeEntity;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxInitiatorTypeEntity;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxStatusEntity;
import com.sitionix.athssox.postgresql.jpa.outbox.OutboxEventJpaRepository;
import com.sitionix.athssox.postgresql.jpa.outbox.OutboxInitiatorTypeJpaRepository;
import com.sitionix.athssox.postgresql.jpa.outbox.OutboxStatusJpaRepository;
import com.sitionix.forge.outbox.core.model.OutboxRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private static final long DEFAULT_INITIATOR_TYPE_ID = 2L;

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final OutboxStatusJpaRepository outboxStatusJpaRepository;
    private final OutboxInitiatorTypeJpaRepository outboxInitiatorTypeJpaRepository;

    @Override
    @Transactional
    public void create(final OutboxRecord outboxRecord) {
        final OutboxEventEntity entity = this.toEntity(outboxRecord);
        if (entity.getInitiatorType() == null) {
            entity.setInitiatorType(this.outboxInitiatorTypeJpaRepository.getReferenceById(DEFAULT_INITIATOR_TYPE_ID));
        }
        this.outboxEventJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public List<OutboxRecord> claimPendingEvents(final List<String> eventStatuses,
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
                .map(this::toOutboxRecord)
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

    @Override
    @Transactional
    public int deleteSentBefore(final LocalDateTime cutoff) {
        return this.outboxEventJpaRepository.deleteSentBefore(cutoff, OutboxStatus.SENT.getId());
    }

    private OutboxEventEntity toEntity(final OutboxRecord outboxRecord) {
        return OutboxEventEntity.builder()
                .aggregateType(this.toAggregateType(outboxRecord.getAggregateType()))
                .aggregateId(outboxRecord.getAggregateId())
                .eventType(this.toEventType(outboxRecord.getEventType()))
                .status(this.toStatus(outboxRecord.getStatus()))
                .initiatorType(this.toInitiatorType(outboxRecord.getInitiatorType()))
                .initiatorId(outboxRecord.getInitiatorId())
                .retryCount(outboxRecord.getAttempts())
                .nextRetryAt(outboxRecord.getNextAttemptAt() == null ? null : LocalDateTime.ofInstant(outboxRecord.getNextAttemptAt(), java.time.ZoneOffset.UTC))
                .payload(outboxRecord.getPayload())
                .lastError(outboxRecord.getLastError())
                .build();
    }

    private OutboxRecord toOutboxRecord(final OutboxEventEntity entity) {
        return OutboxRecord.builder()
                .id(String.valueOf(entity.getId()))
                .eventType(entity.getEventType().getDescription())
                .payload(entity.getPayload())
                .headers(java.util.Map.of())
                .metadata(java.util.Map.of())
                .traceId(null)
                .aggregateType(entity.getAggregateType().getDescription())
                .aggregateId(entity.getAggregateId())
                .initiatorType(entity.getInitiatorType() == null
                        ? InitiatorType.SYSTEM.getDescription()
                        : entity.getInitiatorType().getDescription())
                .initiatorId(entity.getInitiatorId())
                .status(com.sitionix.forge.outbox.core.model.OutboxStatus.valueOf(entity.getStatus().getDescription()))
                .attempts(entity.getRetryCount())
                .nextAttemptAt(entity.getNextRetryAt() == null ? null : entity.getNextRetryAt().toInstant(java.time.ZoneOffset.UTC))
                .lastError(entity.getLastError())
                .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().toInstant(java.time.ZoneOffset.UTC))
                .updatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC))
                .build();
    }

    private OutboxAggregateTypeEntity toAggregateType(final String description) {
        final OutboxAggregateType value = Stream.of(OutboxAggregateType.values())
                .filter(item -> item.getDescription().equals(description))
                .findFirst()
                .orElse(OutboxAggregateType.USER);
        return OutboxAggregateTypeEntity.builder()
                .id(value.getId())
                .description(value.getDescription())
                .build();
    }

    private OutboxEventTypeEntity toEventType(final String description) {
        final OutboxEventType value = Stream.of(OutboxEventType.values())
                .filter(item -> item.getDescription().equals(description))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported event type: " + description));
        return OutboxEventTypeEntity.builder()
                .id(value.getId())
                .description(value.getDescription())
                .build();
    }

    private OutboxStatusEntity toStatus(final com.sitionix.forge.outbox.core.model.OutboxStatus status) {
        final OutboxStatus value = status == null
                ? OutboxStatus.PENDING
                : Stream.of(OutboxStatus.values())
                .filter(item -> item.getDescription().equals(status.name()))
                .findFirst()
                .orElse(OutboxStatus.PENDING);
        return OutboxStatusEntity.builder()
                .id(value.getId())
                .description(value.getDescription())
                .build();
    }

    private OutboxInitiatorTypeEntity toInitiatorType(final String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        final InitiatorType value = Stream.of(InitiatorType.values())
                .filter(item -> item.getDescription().equals(description))
                .findFirst()
                .orElse(InitiatorType.SYSTEM);
        return OutboxInitiatorTypeEntity.builder()
                .id(value.getId())
                .description(value.getDescription())
                .build();
    }
}
