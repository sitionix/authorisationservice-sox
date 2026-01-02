package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.InitiatorType;
import com.sitionix.athssox.domain.model.outbox.payload.handler.EventTypeHandler;
import com.sitionix.athssox.postgresql.entity.OutboxAggregateTypeEntity;
import com.sitionix.athssox.postgresql.entity.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.OutboxEventTypeEntity;
import com.sitionix.athssox.postgresql.entity.OutboxInitiatorTypeEntity;
import com.sitionix.athssox.postgresql.entity.OutboxStatusEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxInfraMapperImplTest {

    private OutboxInfraMapper mapper;

    @Mock
    private OutboxAggregateTypeInfraMapper outboxAggregateTypeInfraMapper;

    @Mock
    private OutboxEventTypeInfraMapper outboxEventTypeInfraMapper;

    @Mock
    private OutboxInitiatorTypeInfraMapper outboxInitiatorTypeInfraMapper;

    @Mock
    private OutboxStatusInfraMapper outboxStatusInfraMapper;

    @Mock
    private OutboxPayloadJsonMapper outboxPayloadJsonMapper;

    private EventTypeHandler<Object> previousHandler;

    @BeforeEach
    void setUp() {
        this.mapper = new OutboxInfraMapperImpl(
                this.outboxAggregateTypeInfraMapper,
                this.outboxEventTypeInfraMapper,
                this.outboxInitiatorTypeInfraMapper,
                this.outboxStatusInfraMapper,
                this.outboxPayloadJsonMapper);
        this.previousHandler = OutboxEventType.EMAIL_VERIFY.getHandler();
    }

    @AfterEach
    void tearDown() {
        OutboxEventType.EMAIL_VERIFY.setHandler(this.previousHandler);
        verifyNoMoreInteractions(this.outboxAggregateTypeInfraMapper,
                this.outboxEventTypeInfraMapper,
                this.outboxInitiatorTypeInfraMapper,
                this.outboxStatusInfraMapper,
                this.outboxPayloadJsonMapper);
    }

    @Test
    void givenOutboxEvent_whenToEntity_thenReturnMappedEntity() {
        //given
        final LocalDateTime dateTime = this.getCreatedAt();
        final EmailVerifyPayload payload = mock(EmailVerifyPayload.class);

        final OutboxEvent<EmailVerifyPayload> given = this.buildOutboxEvent(dateTime, payload);
        final OutboxEventEntity expected = this.buildOutboxEventEntity(dateTime);

        when(this.outboxAggregateTypeInfraMapper.asOutboxAggregateTypeEntity(OutboxAggregateType.USER)).thenReturn(mock(OutboxAggregateTypeEntity.class));
        when(this.outboxEventTypeInfraMapper.asOutboxEventTypeEntity(OutboxEventType.EMAIL_VERIFY)).thenReturn(mock(OutboxEventTypeEntity.class));
        when(this.outboxStatusInfraMapper.asOutboxStatusEntity(OutboxStatus.PENDING)).thenReturn(mock(OutboxStatusEntity.class));
        when(this.outboxPayloadJsonMapper.asJson(payload)).thenReturn("mockedPayload");

        //when
        final OutboxEventEntity actual = this.mapper.toEntity(given);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.outboxAggregateTypeInfraMapper)
                .asOutboxAggregateTypeEntity(OutboxAggregateType.USER);
        verify(this.outboxEventTypeInfraMapper)
                .asOutboxEventTypeEntity(OutboxEventType.EMAIL_VERIFY);
        verify(this.outboxStatusInfraMapper)
                .asOutboxStatusEntity(OutboxStatus.PENDING);
        verify(this.outboxPayloadJsonMapper)
                .asJson(payload);
    }

    @Test
    void givenOutboxEventEntity_whenToOutboxEvent_thenReturnMappedEvent() {
        //given
        final LocalDateTime createdAt = this.getCreatedAt();
        final String payloadJson = this.getPayloadJson();
        final Object payload = mock(Object.class);

        final OutboxAggregateTypeEntity aggregateTypeEntity = this.getOutboxAggregateTypeEntity(1L, "USER");
        final OutboxEventTypeEntity eventTypeEntity = this.getOutboxEventTypeEntity(1L, "EMAIL_VERIFY");
        final OutboxStatusEntity statusEntity = this.getOutboxStatusEntity(1L, "PENDING");
        final OutboxInitiatorTypeEntity initiatorTypeEntity = this.getOutboxInitiatorTypeEntity(1L, "USER");

        final OutboxEventEntity given = this.buildOutboxEventEntity(createdAt,
                aggregateTypeEntity,
                eventTypeEntity,
                statusEntity,
                initiatorTypeEntity,
                payloadJson);
        final OutboxEvent<Object> expected = this.buildOutboxEvent(createdAt, payload);
        final EventTypeHandler<Object> handler = mock(EventTypeHandler.class);

        OutboxEventType.EMAIL_VERIFY.setHandler(handler);

        when(this.outboxAggregateTypeInfraMapper.asEventType(aggregateTypeEntity))
                .thenReturn(OutboxAggregateType.USER);
        when(this.outboxEventTypeInfraMapper.asEventType(eventTypeEntity))
                .thenReturn(OutboxEventType.EMAIL_VERIFY);
        when(this.outboxStatusInfraMapper.asEventType(statusEntity))
                .thenReturn(OutboxStatus.PENDING);
        when(this.outboxInitiatorTypeInfraMapper.asInitiatorType(initiatorTypeEntity))
                .thenReturn(InitiatorType.USER);
        when(handler.getPayload(payloadJson))
                .thenReturn(payload);

        //when
        final OutboxEvent<Object> actual = this.mapper.toOutboxEvent(given);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.outboxAggregateTypeInfraMapper).asEventType(aggregateTypeEntity);
        verify(this.outboxEventTypeInfraMapper, times(2)).asEventType(eventTypeEntity);
        verify(this.outboxStatusInfraMapper).asEventType(statusEntity);
        verify(this.outboxInitiatorTypeInfraMapper).asInitiatorType(initiatorTypeEntity);
        verify(handler).getPayload(payloadJson);
    }

    @Test
    void givenLocalDateTime_whenMap_thenReturnInstant() {
        //given
        final LocalDateTime given = this.getCreatedAt();

        //when
        final Instant actual = this.mapper.map(given);

        //then
        assertThat(actual).isEqualTo(given.atZone(ZoneOffset.UTC).toInstant());
    }

    private OutboxEventEntity buildOutboxEventEntity(final LocalDateTime dateTime) {
        return OutboxEventEntity.builder()
                .eventType(mock(OutboxEventTypeEntity.class))
                .aggregateType(mock(OutboxAggregateTypeEntity.class))
                .aggregateId(1L)
                .eventType(mock(OutboxEventTypeEntity.class))
                .status(mock(OutboxStatusEntity.class))
                .retryCount(0)
                .nextRetryAt(dateTime.plusMinutes(5))
                .payload("mockedPayload")
                .lastError("error")
                .build();
    }

    private OutboxEventEntity buildOutboxEventEntity(final LocalDateTime createdAt,
                                                     final OutboxAggregateTypeEntity aggregateTypeEntity,
                                                     final OutboxEventTypeEntity eventTypeEntity,
                                                     final OutboxStatusEntity statusEntity,
                                                     final OutboxInitiatorTypeEntity initiatorTypeEntity,
                                                     final String payloadJson) {
        return OutboxEventEntity.builder()
                .id(7L)
                .aggregateType(aggregateTypeEntity)
                .aggregateId(11L)
                .eventType(eventTypeEntity)
                .status(statusEntity)
                .initiatorType(initiatorTypeEntity)
                .initiatorId("initiator-1")
                .retryCount(2)
                .nextRetryAt(createdAt.plusMinutes(10))
                .payload(payloadJson)
                .lastError("error")
                .createdAt(createdAt)
                .build();
    }

    private OutboxEvent<Object> buildOutboxEvent(final LocalDateTime createdAt,
                                                 final Object payload) {
        return OutboxEvent.builder()
                .id(7L)
                .aggregateType(OutboxAggregateType.USER)
                .aggregateId(11L)
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .status(OutboxStatus.PENDING)
                .initiatorType(InitiatorType.USER)
                .initiatorId("initiator-1")
                .retryCount(2)
                .nextRetryAt(createdAt.plusMinutes(10))
                .payload(payload)
                .lastError("error")
                .createdAt(createdAt.atZone(ZoneOffset.UTC).toInstant())
                .build();
    }

    private OutboxEvent<EmailVerifyPayload> buildOutboxEvent(final LocalDateTime dateTime, final EmailVerifyPayload payload) {
        return OutboxEvent.<EmailVerifyPayload>builder()
                .aggregateType(OutboxAggregateType.USER)
                .aggregateId(1L)
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(dateTime.plusMinutes(5))
                .payload(payload)
                .lastError("error")
                .build();
    }

    private OutboxAggregateTypeEntity getOutboxAggregateTypeEntity(final Long id, final String description) {
        return OutboxAggregateTypeEntity.builder()
                .id(id)
                .description(description)
                .build();
    }

    private OutboxEventTypeEntity getOutboxEventTypeEntity(final Long id, final String description) {
        return OutboxEventTypeEntity.builder()
                .id(id)
                .description(description)
                .build();
    }

    private OutboxStatusEntity getOutboxStatusEntity(final Long id, final String description) {
        return OutboxStatusEntity.builder()
                .id(id)
                .description(description)
                .build();
    }

    private OutboxInitiatorTypeEntity getOutboxInitiatorTypeEntity(final Long id, final String description) {
        return OutboxInitiatorTypeEntity.builder()
                .id(id)
                .description(description)
                .build();
    }

    private LocalDateTime getCreatedAt() {
        return LocalDateTime.of(2024, 4, 25, 10, 15, 30);
    }

    private String getPayloadJson() {
        return "{\"payload\":\"value\"}";
    }
}
