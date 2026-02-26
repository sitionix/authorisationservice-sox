package com.sitionix.athssox.application.command.outbox;

import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.InitiatorType;
import com.sitionix.forge.outbox.core.model.OutboxEnqueueRequest;
import com.sitionix.forge.outbox.core.port.ForgeOutbox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerifyOutboxCommandTest {

    private EmailVerifyOutboxCommand emailVerifyOutboxCommand;

    @Mock
    private ForgeOutbox forgeOutbox;

    @BeforeEach
    void setUp() {
        this.emailVerifyOutboxCommand = new EmailVerifyOutboxCommand(this.forgeOutbox);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.forgeOutbox);
    }

    @Test
    void givenOutboxEvent_whenExecute_thenEnqueueForgeOutboxRequest() throws Exception {
        //given
        final EmailVerifyPayload payload = this.getEmailVerifyPayload();
        final OutboxEvent<EmailVerifyPayload> outboxEvent = this.getOutboxEvent(payload);
        final ArgumentCaptor<OutboxEnqueueRequest> outboxEnqueueRequestCaptor = ArgumentCaptor.forClass(OutboxEnqueueRequest.class);

        //when
        this.emailVerifyOutboxCommand.execute(outboxEvent);

        //then
        verify(this.forgeOutbox).enqueue(outboxEnqueueRequestCaptor.capture());

        final OutboxEnqueueRequest actual = outboxEnqueueRequestCaptor.getValue();
        assertThat(actual.getEventType()).isEqualTo("EMAIL_VERIFY");
        assertThat(actual.getPayloadObject()).isEqualTo(payload);
        assertThat(actual.getAggregateType()).isEqualTo("USER");
        assertThat(actual.getAggregateId()).isEqualTo(5L);
        assertThat(actual.getInitiatorType()).isEqualTo("USER");
        assertThat(actual.getInitiatorId()).isEqualTo("5");
        assertThat(actual.getTraceId()).isEqualTo("trace-1");
    }

    private OutboxEvent<EmailVerifyPayload> getOutboxEvent(final EmailVerifyPayload payload) {
        return OutboxEvent.<EmailVerifyPayload>builder()
                .id(10L)
                .aggregateType(OutboxAggregateType.USER)
                .aggregateId(5L)
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .initiatorType(InitiatorType.USER)
                .initiatorId("5")
                .payload(payload)
                .nextRetryAt(LocalDateTime.parse("2026-01-01T10:00:00"))
                .build();
    }

    private EmailVerifyPayload getEmailVerifyPayload() {
        final EmailVerifyPayload.Meta meta = mock(EmailVerifyPayload.Meta.class);
        when(meta.getUserId()).thenReturn(5L);
        when(meta.getSiteId()).thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        when(meta.getTraceId()).thenReturn("trace-1");
        when(meta.getRequestedAt()).thenReturn(Instant.parse("2026-01-01T10:00:00Z"));

        final EmailVerifyPayload payload = mock(EmailVerifyPayload.class);
        when(payload.getMeta()).thenReturn(meta);

        return payload;
    }
}
