package com.sitionix.athssox.pipe.producer;

import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.app_afesox.ntfssox.events.notifications.kafka.NotificationsV1Producer;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import com.sitionix.athssox.pipe.producer.mapper.NotificationEventMapper;
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
class NotificationPublisherV1Test {

    private NotificationPublisherV1 notificationPublisherV1;

    @Mock
    private NotificationsV1Producer producer;

    @Mock
    private NotificationEventMapper mapper;

    @BeforeEach
    void setUp() {
        this.notificationPublisherV1 = new NotificationPublisherV1(this.producer, this.mapper);
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
        this.notificationPublisherV1.publish(given);

        //then
        verify(this.mapper, never()).asEnvelope(given);
    }

    @Test
    void givenEvent_whenPublish_thenSendEnvelope() {
        //given
        final Event<EmailVerifyPayload> given = mock(Event.class);
        final NotificationEnvelope envelope = mock(NotificationEnvelope.class);
        final String eventId = "event-1";

        when(given.getId())
                .thenReturn(eventId);
        when(this.mapper.asEnvelope(given))
                .thenReturn(envelope);

        //when
        this.notificationPublisherV1.publish(given);

        //then
        verify(this.mapper).asEnvelope(given);
        verify(given).getId();
        verify(this.producer).send(eventId, envelope);
        verifyNoMoreInteractions(given, envelope);
    }
}
