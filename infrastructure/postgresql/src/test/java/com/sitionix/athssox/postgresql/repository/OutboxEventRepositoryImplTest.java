package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxAggregateTypeEntity;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventTypeEntity;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxInitiatorTypeEntity;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxStatusEntity;
import com.sitionix.athssox.postgresql.jpa.outbox.OutboxEventJpaRepository;
import com.sitionix.athssox.postgresql.jpa.outbox.OutboxInitiatorTypeJpaRepository;
import com.sitionix.athssox.postgresql.jpa.outbox.OutboxStatusJpaRepository;
import com.sitionix.athssox.postgresql.repository.outbox.OutboxEventRepositoryImpl;
import com.sitionix.forge.outbox.core.model.OutboxRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventRepositoryImplTest {

    private OutboxEventRepositoryImpl repository;

    @Mock
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Mock
    private OutboxStatusJpaRepository outboxStatusJpaRepository;

    @Mock
    private OutboxInitiatorTypeJpaRepository outboxInitiatorTypeJpaRepository;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Captor
    private ArgumentCaptor<OutboxEventEntity> outboxEventEntityCaptor;

    @BeforeEach
    void setUp() {
        this.repository = new OutboxEventRepositoryImpl(this.outboxEventJpaRepository,
                this.outboxStatusJpaRepository,
                this.outboxInitiatorTypeJpaRepository);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.outboxEventJpaRepository,
                this.outboxStatusJpaRepository,
                this.outboxInitiatorTypeJpaRepository);
    }

    @Test
    void givenOutboxRecordWithoutInitiator_whenCreate_thenSetDefaultInitiatorType() {
        //given
        final OutboxRecord given = this.getOutboxRecord(null, 0);
        final OutboxInitiatorTypeEntity initiatorType = this.getOutboxInitiatorTypeEntity(2L, "SYSTEM");

        when(this.outboxInitiatorTypeJpaRepository.getReferenceById(2L))
                .thenReturn(initiatorType);

        //when
        this.repository.create(given);

        //then
        verify(this.outboxInitiatorTypeJpaRepository)
                .getReferenceById(2L);
        verify(this.outboxEventJpaRepository)
                .save(this.outboxEventEntityCaptor.capture());

        final OutboxEventEntity actual = this.outboxEventEntityCaptor.getValue();
        assertThat(actual.getAggregateType().getDescription()).isEqualTo("USER");
        assertThat(actual.getAggregateId()).isEqualTo(1L);
        assertThat(actual.getEventType().getDescription()).isEqualTo("EMAIL_VERIFY");
        assertThat(actual.getStatus().getDescription()).isEqualTo("PENDING");
        assertThat(actual.getInitiatorType()).isEqualTo(initiatorType);
        assertThat(actual.getInitiatorId()).isEqualTo("1");
        assertThat(actual.getPayload()).isEqualTo("payload");
    }

    @Test
    void givenOutboxRecordWithInitiator_whenCreate_thenPersistWithoutDefaultLookup() {
        //given
        final OutboxRecord given = this.getOutboxRecord("USER", 0);

        //when
        this.repository.create(given);

        //then
        verify(this.outboxEventJpaRepository)
                .save(this.outboxEventEntityCaptor.capture());

        final OutboxEventEntity actual = this.outboxEventEntityCaptor.getValue();
        assertThat(actual.getInitiatorType().getDescription()).isEqualTo("USER");
        assertThat(actual.getInitiatorType().getId()).isEqualTo(1L);
    }

    @Test
    void givenEmptyEventTypes_whenClaimPendingEvents_thenReturnEmptyList() {
        //given
        final List<String> eventTypes = List.of();

        //when
        final List<OutboxRecord> actual = this.repository.claimPendingEvents(List.of("PENDING"),
                eventTypes,
                5,
                LocalDateTime.now());

        //then
        assertThat(actual).isEqualTo(List.of());
    }

    @Test
    void givenPendingEvents_whenClaimPendingEvents_thenReturnMappedRecordsAndUpdateStatus() {
        //given
        final List<String> statuses = List.of("PENDING", "FAILED");
        final List<String> eventTypes = List.of("EMAIL_VERIFY");
        final LocalDateTime now = LocalDateTime.now();
        final int batchSize = 2;

        final OutboxStatusEntity inProgress = this.getOutboxStatusEntity(OutboxStatus.IN_PROGRESS);
        final OutboxEventEntity firstEntity = this.getOutboxEventEntity(null, 0, null);
        final OutboxEventEntity secondEntity = this.getOutboxEventEntity(this.getOutboxInitiatorTypeEntity(1L, "USER"), 1, "error");
        final List<OutboxEventEntity> entities = List.of(firstEntity, secondEntity);

        when(this.outboxEventJpaRepository.findPendingForUpdate(eq(statuses),
                eq(eventTypes),
                eq(now),
                this.pageableCaptor.capture()))
                .thenReturn(entities);
        when(this.outboxStatusJpaRepository.getReferenceById(OutboxStatus.IN_PROGRESS.getId()))
                .thenReturn(inProgress);
        when(this.outboxEventJpaRepository.saveAll(entities))
                .thenReturn(entities);

        //when
        final List<OutboxRecord> actual = this.repository.claimPendingEvents(statuses,
                eventTypes,
                batchSize,
                now);

        //then
        assertThat(actual).hasSize(2);
        assertThat(actual.get(0).getStatus()).isEqualTo(com.sitionix.forge.outbox.core.model.OutboxStatus.IN_PROGRESS);
        assertThat(actual.get(0).getInitiatorType()).isEqualTo("SYSTEM");
        assertThat(actual.get(1).getLastError()).isEqualTo("error");
        assertThat(actual.get(1).getInitiatorType()).isEqualTo("USER");
        assertThat(firstEntity.getStatus()).isEqualTo(inProgress);
        assertThat(secondEntity.getStatus()).isEqualTo(inProgress);
        assertThat(this.pageableCaptor.getValue().getPageSize()).isEqualTo(batchSize);

        verify(this.outboxEventJpaRepository)
                .findPendingForUpdate(eq(statuses),
                        eq(eventTypes),
                        eq(now),
                        any(Pageable.class));
        verify(this.outboxStatusJpaRepository)
                .getReferenceById(OutboxStatus.IN_PROGRESS.getId());
        verify(this.outboxEventJpaRepository)
                .saveAll(entities);
    }

    @Test
    void givenExistingEvent_whenMarkSent_thenUpdateStatusAndClearError() {
        //given
        final Long eventId = 11L;
        final OutboxStatusEntity sentStatus = this.getOutboxStatusEntity(OutboxStatus.SENT);
        final OutboxEventEntity entity = this.getOutboxEventEntity(null, 0, "error");

        when(this.outboxEventJpaRepository.findById(eventId))
                .thenReturn(Optional.of(entity));
        when(this.outboxStatusJpaRepository.getReferenceById(OutboxStatus.SENT.getId()))
                .thenReturn(sentStatus);

        //when
        this.repository.markSent(eventId);

        //then
        assertThat(entity.getStatus()).isEqualTo(sentStatus);
        assertThat(entity.getLastError()).isNull();

        verify(this.outboxEventJpaRepository)
                .findById(eventId);
        verify(this.outboxStatusJpaRepository)
                .getReferenceById(OutboxStatus.SENT.getId());
        verify(this.outboxEventJpaRepository)
                .save(entity);
    }

    @Test
    void givenRetryBelowLimit_whenMarkFailed_thenSetFailedStatusAndIncrement() {
        //given
        final Long eventId = 12L;
        final Duration retryDelay = Duration.ofSeconds(10);
        final OutboxStatusEntity failedStatus = this.getOutboxStatusEntity(OutboxStatus.FAILED);
        final OutboxEventEntity entity = this.getOutboxEventEntity(null, 0, null);
        final LocalDateTime before = LocalDateTime.now();

        when(this.outboxEventJpaRepository.findById(eventId))
                .thenReturn(Optional.of(entity));
        when(this.outboxStatusJpaRepository.getReferenceById(OutboxStatus.FAILED.getId()))
                .thenReturn(failedStatus);

        //when
        this.repository.markFailed(eventId, "boom", retryDelay, 3);

        //then
        assertThat(entity.getRetryCount()).isEqualTo(1);
        assertThat(entity.getLastError()).isEqualTo("boom");
        assertThat(entity.getNextRetryAt()).isAfterOrEqualTo(before.plus(retryDelay));
        assertThat(entity.getStatus()).isEqualTo(failedStatus);

        verify(this.outboxEventJpaRepository)
                .findById(eventId);
        verify(this.outboxStatusJpaRepository)
                .getReferenceById(OutboxStatus.FAILED.getId());
        verify(this.outboxEventJpaRepository)
                .save(entity);
    }

    @Test
    void givenRetryAtLimit_whenMarkFailed_thenSetDeadStatusAndIncrement() {
        //given
        final Long eventId = 13L;
        final Duration retryDelay = Duration.ofSeconds(5);
        final OutboxStatusEntity deadStatus = this.getOutboxStatusEntity(OutboxStatus.DEAD);
        final OutboxEventEntity entity = this.getOutboxEventEntity(null, 1, null);
        final LocalDateTime before = LocalDateTime.now();

        when(this.outboxEventJpaRepository.findById(eventId))
                .thenReturn(Optional.of(entity));
        when(this.outboxStatusJpaRepository.getReferenceById(OutboxStatus.DEAD.getId()))
                .thenReturn(deadStatus);

        //when
        this.repository.markFailed(eventId, "failure", retryDelay, 2);

        //then
        assertThat(entity.getRetryCount()).isEqualTo(2);
        assertThat(entity.getLastError()).isEqualTo("failure");
        assertThat(entity.getNextRetryAt()).isAfterOrEqualTo(before.plus(retryDelay));
        assertThat(entity.getStatus()).isEqualTo(deadStatus);

        verify(this.outboxEventJpaRepository)
                .findById(eventId);
        verify(this.outboxStatusJpaRepository)
                .getReferenceById(OutboxStatus.DEAD.getId());
        verify(this.outboxEventJpaRepository)
                .save(entity);
    }

    @Test
    void givenCutoff_whenDeleteSentBefore_thenDelegateToRepository() {
        //given
        final LocalDateTime cutoff = LocalDateTime.now();

        when(this.outboxEventJpaRepository.deleteSentBefore(cutoff, OutboxStatus.SENT.getId()))
                .thenReturn(3);

        //when
        final int actual = this.repository.deleteSentBefore(cutoff);

        //then
        assertThat(actual).isEqualTo(3);
        verify(this.outboxEventJpaRepository)
                .deleteSentBefore(cutoff, OutboxStatus.SENT.getId());
    }

    private OutboxEventEntity getOutboxEventEntity(final OutboxInitiatorTypeEntity initiatorType,
                                                   final int retryCount,
                                                   final String lastError) {
        return OutboxEventEntity.builder()
                .id(15L)
                .aggregateType(OutboxAggregateTypeEntity.builder()
                        .id(1L)
                        .description("USER")
                        .build())
                .aggregateId(1L)
                .eventType(OutboxEventTypeEntity.builder()
                        .id(1L)
                        .description("EMAIL_VERIFY")
                        .build())
                .status(this.getOutboxStatusEntity(OutboxStatus.PENDING))
                .initiatorType(initiatorType)
                .initiatorId("1")
                .retryCount(retryCount)
                .nextRetryAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .payload("payload")
                .lastError(lastError)
                .createdAt(LocalDateTime.of(2024, 1, 1, 9, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 1, 9, 30))
                .build();
    }

    private OutboxStatusEntity getOutboxStatusEntity(final OutboxStatus status) {
        return OutboxStatusEntity.builder()
                .id(status.getId())
                .description(status.getDescription())
                .build();
    }

    private OutboxInitiatorTypeEntity getOutboxInitiatorTypeEntity(final Long id, final String description) {
        return OutboxInitiatorTypeEntity.builder()
                .id(id)
                .description(description)
                .build();
    }

    private OutboxRecord getOutboxRecord(final String initiatorType, final int attempts) {
        return OutboxRecord.builder()
                .eventType("EMAIL_VERIFY")
                .payload("payload")
                .aggregateType("USER")
                .aggregateId(1L)
                .initiatorType(initiatorType)
                .initiatorId("1")
                .status(com.sitionix.forge.outbox.core.model.OutboxStatus.PENDING)
                .attempts(attempts)
                .nextAttemptAt(Instant.parse("2024-01-01T10:00:00Z"))
                .build();
    }
}
