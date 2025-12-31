package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.postgresql.entity.OutboxAggregateTypeEntity;
import com.sitionix.athssox.postgresql.entity.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.OutboxEventTypeEntity;
import com.sitionix.athssox.postgresql.entity.OutboxStatusEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

    @BeforeEach
    void setUp() {
        this.mapper = new OutboxInfraMapperImpl(
                this.outboxAggregateTypeInfraMapper,
                this.outboxEventTypeInfraMapper,
                this.outboxInitiatorTypeInfraMapper,
                this.outboxStatusInfraMapper,
                this.outboxPayloadJsonMapper);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.outboxAggregateTypeInfraMapper,
                this.outboxEventTypeInfraMapper,
                this.outboxInitiatorTypeInfraMapper,
                this.outboxStatusInfraMapper,
                this.outboxPayloadJsonMapper);
    }

    @Test
    void given_outbox_event_when_to_entity_then_return_mapped_entity() {
        //given
        final LocalDateTime dateTime = LocalDateTime.now();
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
}
