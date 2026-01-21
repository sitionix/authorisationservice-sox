package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxInitiatorTypeEntity;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxStatusEntity;
import com.sitionix.athssox.postgresql.jpa.outbox.OutboxEventJpaRepository;
import com.sitionix.athssox.postgresql.jpa.outbox.OutboxInitiatorTypeJpaRepository;
import com.sitionix.athssox.postgresql.jpa.outbox.OutboxStatusJpaRepository;
import com.sitionix.athssox.postgresql.mapper.outbox.OutboxInfraMapper;
import com.sitionix.athssox.postgresql.repository.outbox.OutboxEventRepositoryImpl;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

    @Mock
    private OutboxInfraMapper outboxInfraMapper;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @BeforeEach
    void setUp() {
        this.repository = new OutboxEventRepositoryImpl(this.outboxEventJpaRepository,
                this.outboxStatusJpaRepository,
                this.outboxInitiatorTypeJpaRepository,
                this.outboxInfraMapper);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.outboxEventJpaRepository,
                this.outboxStatusJpaRepository,
                this.outboxInitiatorTypeJpaRepository,
                this.outboxInfraMapper);
    }

    @Test
    void given_outbox_event_without_initiator_when_create_then_set_default_initiator_type() {
        //given
        final OutboxEvent<?> given = mock(OutboxEvent.class);
        final OutboxEventEntity entity = this.getOutboxEventEntity(null, 0, null);
        final OutboxInitiatorTypeEntity initiatorType = this.getOutboxInitiatorTypeEntity(2L);

        when(this.outboxInfraMapper.toEntity(given))
                .thenReturn(entity);
        when(this.outboxInitiatorTypeJpaRepository.getReferenceById(2L))
                .thenReturn(initiatorType);

        //when
        this.repository.create(given);

        //then
        assertThat(entity.getInitiatorType()).isEqualTo(initiatorType);
        verify(this.outboxInfraMapper)
                .toEntity(given);
        verify(this.outboxInitiatorTypeJpaRepository)
                .getReferenceById(2L);
        verify(this.outboxEventJpaRepository)
                .save(entity);
    }

    @Test
    void given_outbox_event_with_initiator_when_create_then_persist_without_default() {
        //given
        final OutboxEvent<?> given = mock(OutboxEvent.class);
        final OutboxInitiatorTypeEntity initiatorType = this.getOutboxInitiatorTypeEntity(1L);
        final OutboxEventEntity entity = this.getOutboxEventEntity(initiatorType, 0, null);

        when(this.outboxInfraMapper.toEntity(given))
                .thenReturn(entity);

        //when
        this.repository.create(given);

        //then
        assertThat(entity.getInitiatorType()).isEqualTo(initiatorType);
        verify(this.outboxInfraMapper)
                .toEntity(given);
        verify(this.outboxEventJpaRepository)
                .save(entity);
    }

    @Test
    void given_empty_event_types_when_claim_pending_events_then_return_empty_list() {
        //given
        final List<String> eventTypes = List.of();

        //when
        final List<OutboxEvent<Object>> actual = this.repository.claimPendingEvents(List.of("PENDING"),
                eventTypes,
                5,
                LocalDateTime.now());

        //then
        assertThat(actual).isEqualTo(List.of());
    }

    @Test
    void given_pending_events_when_claim_pending_events_then_return_mapped_and_update_status() {
        //given
        final List<String> statuses = List.of("PENDING", "FAILED");
        final List<String> eventTypes = List.of("EMAIL_VERIFY");
        final LocalDateTime now = LocalDateTime.now();
        final int batchSize = 2;

        final OutboxStatusEntity inProgress = this.getOutboxStatusEntity(OutboxStatus.IN_PROGRESS);
        final OutboxEventEntity firstEntity = this.getOutboxEventEntity(null, 0, null);
        final OutboxEventEntity secondEntity = this.getOutboxEventEntity(null, 1, "error");
        final List<OutboxEventEntity> entities = List.of(firstEntity, secondEntity);

        final OutboxEvent<Object> firstEvent = mock(OutboxEvent.class);
        final OutboxEvent<Object> secondEvent = mock(OutboxEvent.class);
        final List<OutboxEvent<Object>> expected = List.of(firstEvent, secondEvent);

        when(this.outboxEventJpaRepository.findPendingForUpdate(eq(statuses),
                eq(eventTypes),
                eq(now),
                this.pageableCaptor.capture()))
                .thenReturn(entities);
        when(this.outboxStatusJpaRepository.getReferenceById(OutboxStatus.IN_PROGRESS.getId()))
                .thenReturn(inProgress);
        when(this.outboxEventJpaRepository.saveAll(entities))
                .thenReturn(entities);
        when(this.outboxInfraMapper.toOutboxEvent(firstEntity))
                .thenReturn(firstEvent);
        when(this.outboxInfraMapper.toOutboxEvent(secondEntity))
                .thenReturn(secondEvent);

        //when
        final List<OutboxEvent<Object>> actual = this.repository.claimPendingEvents(statuses,
                eventTypes,
                batchSize,
                now);

        //then
        assertThat(actual).isEqualTo(expected);
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
        verify(this.outboxInfraMapper)
                .toOutboxEvent(firstEntity);
        verify(this.outboxInfraMapper)
                .toOutboxEvent(secondEntity);
    }

    @Test
    void given_existing_event_when_mark_sent_then_update_status_and_clear_error() {
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
    void given_retry_below_limit_when_mark_failed_then_set_failed_status_and_increment() {
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
    void given_retry_at_limit_when_mark_failed_then_set_dead_status_and_increment() {
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
    void given_cutoff_when_delete_sent_before_then_delegate_to_repository() {
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
                .initiatorType(initiatorType)
                .retryCount(retryCount)
                .lastError(lastError)
                .build();
    }

    private OutboxStatusEntity getOutboxStatusEntity(final OutboxStatus status) {
        return OutboxStatusEntity.builder()
                .id(status.getId())
                .description(status.getDescription())
                .build();
    }

    private OutboxInitiatorTypeEntity getOutboxInitiatorTypeEntity(final Long id) {
        return OutboxInitiatorTypeEntity.builder()
                .id(id)
                .description("SYSTEM")
                .build();
    }
}
