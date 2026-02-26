package com.sitionix.athssox.application.outbox.handler;

import com.sitionix.athssox.domain.event.EventHandler;
import com.sitionix.athssox.domain.exception.OutboxPayloadParseException;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import com.sitionix.forge.outbox.core.port.OutboxPayloadCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerifyHandlerTest {

    private EmailVerifyHandler emailVerifyHandler;

    @Mock
    private OutboxPayloadCodec outboxPayloadCodec;

    @Mock
    private EventHandler<EmailVerifyPayload> eventHandler;

    @BeforeEach
    void setUp() {
        this.emailVerifyHandler = new EmailVerifyHandler(this.outboxPayloadCodec, this.eventHandler);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.outboxPayloadCodec,
                this.eventHandler);
    }

    @Test
    void given_outbox_event_when_do_handle_then_publish_event() {
        //given
        final Instant createdAt = this.getCreatedAt();
        final EmailVerifyPayload payload = mock(EmailVerifyPayload.class);
        final OutboxEvent<EmailVerifyPayload> given = this.getOutboxEvent(payload, createdAt);
        final ArgumentCaptor<Event<EmailVerifyPayload>> captor = ArgumentCaptor.forClass(Event.class);

        //when
        this.emailVerifyHandler.doHandle(given);

        //then
        verify(this.eventHandler).publish(captor.capture());

        final Event<EmailVerifyPayload> actual = captor.getValue();
        assertThat(actual.getPayload()).isEqualTo(payload);
        assertThat(actual.getEventType()).isEqualTo(OutboxEventType.EMAIL_VERIFY.getDescription());
        assertThat(actual.getCreatedAt()).isEqualTo(createdAt);
        assertThat(actual.getId()).isEqualTo(given.getId().toString());
        assertThat(actual.getIdempotencyId()).isNotNull();
    }

    @Test
    void given_payload_json_when_get_payload_then_return_email_verify_payload() throws Exception {
        //given
        final String payload = this.getPayloadJson();
        final EmailVerifyPayload expected = mock(EmailVerifyPayload.class);

        when(this.outboxPayloadCodec.deserialize(payload, EmailVerifyPayload.class))
                .thenReturn(expected);

        //when
        final EmailVerifyPayload actual = this.emailVerifyHandler.getPayload(payload);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.outboxPayloadCodec).deserialize(payload, EmailVerifyPayload.class);
    }

    @Test
    void given_invalid_payload_when_get_payload_then_throw_outbox_payload_parse_exception() {
        //given
        final String payload = this.getPayloadJson();
        final IllegalStateException exception = new IllegalStateException("boom");

        when(this.outboxPayloadCodec.deserialize(payload, EmailVerifyPayload.class))
                .thenThrow(exception);

        //when
        //then
        assertThatThrownBy(() -> this.emailVerifyHandler.getPayload(payload))
                .isInstanceOf(OutboxPayloadParseException.class)
                .hasMessageContaining("Payload cannot be parsed into EmailVerifyPayload due to error: " + exception);
        verify(this.outboxPayloadCodec).deserialize(payload, EmailVerifyPayload.class);
    }

    private OutboxEvent<EmailVerifyPayload> getOutboxEvent(final EmailVerifyPayload payload,
                                                           final Instant createdAt) {
        return OutboxEvent.<EmailVerifyPayload>builder()
                .id(1L)
                .payload(payload)
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .createdAt(createdAt)
                .build();
    }

    private String getPayloadJson() {
        return "payload";
    }

    private Instant getCreatedAt() {
        return Instant.parse("2024-04-25T10:15:30Z");
    }
}
