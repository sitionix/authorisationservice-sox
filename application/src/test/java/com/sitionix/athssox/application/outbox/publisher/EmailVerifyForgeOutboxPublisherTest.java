package com.sitionix.athssox.application.outbox.publisher;

import com.sitionix.athssox.domain.event.EventHandler;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import com.sitionix.forge.outbox.core.model.OutboxRecord;
import com.sitionix.forge.outbox.core.port.OutboxPayloadCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerifyForgeOutboxPublisherTest {

    private EmailVerifyForgeOutboxPublisher emailVerifyForgeOutboxPublisher;

    @Mock
    private OutboxPayloadCodec outboxPayloadCodec;

    @Mock
    private EventHandler<EmailVerifyPayload> eventHandler;

    @BeforeEach
    void setUp() {
        this.emailVerifyForgeOutboxPublisher = new EmailVerifyForgeOutboxPublisher(this.outboxPayloadCodec, this.eventHandler);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.outboxPayloadCodec, this.eventHandler);
    }

    @Test
    void givenOutboxRecord_whenPublish_thenForwardMappedEventToEventHandler() throws Exception {
        //given
        final EmailVerifyPayload payload = mock(EmailVerifyPayload.class);
        final OutboxRecord outboxRecord = this.getOutboxRecord();
        final ArgumentCaptor<Event<EmailVerifyPayload>> eventCaptor = ArgumentCaptor.forClass(Event.class);

        when(this.outboxPayloadCodec.deserialize(outboxRecord.getPayload(), EmailVerifyPayload.class))
                .thenReturn(payload);

        //when
        this.emailVerifyForgeOutboxPublisher.publish(outboxRecord);

        //then
        verify(this.outboxPayloadCodec).deserialize(outboxRecord.getPayload(), EmailVerifyPayload.class);
        verify(this.eventHandler).publish(eventCaptor.capture());

        final Event<EmailVerifyPayload> actual = eventCaptor.getValue();
        assertThat(actual.getId()).isEqualTo("10");
        assertThat(actual.getEventType()).isEqualTo("EMAIL_VERIFY");
        assertThat(actual.getPayload()).isEqualTo(payload);
    }

    @Test
    void givenPublisher_whenSupportedEventTypes_thenReturnEmailVerifyOnly() {
        //given

        //when
        final Set<String> actual = this.emailVerifyForgeOutboxPublisher.supportedEventTypes();

        //then
        assertThat(actual).isEqualTo(Set.of("EMAIL_VERIFY"));
    }

    private OutboxRecord getOutboxRecord() {
        return OutboxRecord.builder()
                .id("10")
                .eventType("EMAIL_VERIFY")
                .payload("{}")
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }
}
