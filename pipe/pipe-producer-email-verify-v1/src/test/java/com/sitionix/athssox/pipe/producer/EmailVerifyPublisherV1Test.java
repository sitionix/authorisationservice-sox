package com.sitionix.athssox.pipe.producer;

import com.app_afesox.athssox.events.emailverify.EmailVerifyEventEnvelope;
import com.app_afesox.athssox.events.emailverify.kafka.EmailverifyV1Producer;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import com.sitionix.athssox.pipe.producer.mapper.EmailVerifyEventMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerifyPublisherV1Test {

    private EmailVerifyPublisherV1 emailVerifyPublisherV1;

    @Mock
    private EmailverifyV1Producer producer;

    @Mock
    private EmailVerifyEventMapper mapper;

    @BeforeEach
    void setUp() {
        this.emailVerifyPublisherV1 = new EmailVerifyPublisherV1(this.producer, this.mapper);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.producer,
                this.mapper);
    }

    @Test
    void givenNullEvent_whenPublish_thenSkip() {
        //given
        final Event<EmailVerifyPayload> given = null;

        //when
        this.emailVerifyPublisherV1.publish(given);

        //then
        verify(this.mapper, never()).asEnvelope(given);
    }

    @Test
    void givenEvent_whenPublish_thenSendEnvelope() {
        //given
        final Event<EmailVerifyPayload> given = mock(Event.class);
        final EmailVerifyEventEnvelope envelope = mock(EmailVerifyEventEnvelope.class);
        final String eventId = "event-1";

        when(given.getId())
                .thenReturn(eventId);
        when(this.mapper.asEnvelope(given))
                .thenReturn(envelope);

        //when
        this.emailVerifyPublisherV1.publish(given);

        //then
        verify(this.mapper).asEnvelope(given);
        verify(this.producer).send(eventId, envelope);
    }
}
