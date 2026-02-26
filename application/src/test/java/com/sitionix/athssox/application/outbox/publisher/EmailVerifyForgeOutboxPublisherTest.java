package com.sitionix.athssox.application.outbox.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitionix.athssox.domain.event.EventHandler;
import com.sitionix.athssox.domain.exception.OutboxPayloadParseException;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import com.sitionix.forge.outbox.core.model.OutboxRecord;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerifyForgeOutboxPublisherTest {

    private EmailVerifyForgeOutboxPublisher emailVerifyForgeOutboxPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventHandler<EmailVerifyPayload> eventHandler;

    @BeforeEach
    void setUp() {
        this.emailVerifyForgeOutboxPublisher = new EmailVerifyForgeOutboxPublisher(this.objectMapper, this.eventHandler);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.objectMapper, this.eventHandler);
    }

    @Test
    void givenOutboxRecord_whenPublish_thenForwardMappedEventToEventHandler() throws Exception {
        //given
        final EmailVerifyPayload payload = mock(EmailVerifyPayload.class);
        final OutboxRecord outboxRecord = this.getOutboxRecord();
        final ArgumentCaptor<Event<EmailVerifyPayload>> eventCaptor = ArgumentCaptor.forClass(Event.class);

        when(this.objectMapper.readValue(outboxRecord.getPayload(), EmailVerifyPayload.class))
                .thenReturn(payload);

        //when
        this.emailVerifyForgeOutboxPublisher.publish(outboxRecord);

        //then
        verify(this.objectMapper).readValue(outboxRecord.getPayload(), EmailVerifyPayload.class);
        verify(this.eventHandler).publish(eventCaptor.capture());

        final Event<EmailVerifyPayload> actual = eventCaptor.getValue();
        assertThat(actual.getId()).isEqualTo("10");
        assertThat(actual.getEventType()).isEqualTo("EMAIL_VERIFY");
        assertThat(actual.getPayload()).isEqualTo(payload);
    }

    @Test
    void givenInvalidPayload_whenPublish_thenThrowOutboxPayloadParseException() throws Exception {
        //given
        final OutboxRecord outboxRecord = this.getOutboxRecord();
        when(this.objectMapper.readValue(outboxRecord.getPayload(), EmailVerifyPayload.class))
                .thenThrow(new JsonProcessingException("boom") {
                });

        //when
        //then
        assertThatThrownBy(() -> this.emailVerifyForgeOutboxPublisher.publish(outboxRecord))
                .isInstanceOf(OutboxPayloadParseException.class)
                .hasMessageContaining("Payload cannot be parsed into EmailVerifyPayload");

        verify(this.objectMapper).readValue(outboxRecord.getPayload(), EmailVerifyPayload.class);
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
