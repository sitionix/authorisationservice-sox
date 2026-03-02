package com.sitionix.athssox.pipe.producer;

import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.app_afesox.ntfssox.events.notifications.kafka.NotificationsV1Producer;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.pipe.producer.mapper.NotificationEventMapper;
import com.sitionix.forge.outbox.core.model.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void givenNullPayload_whenPublish_thenThrowException() {
        //given
        final Event<EmailVerifyPayload> event = mock(Event.class);

        //when
        //then
        assertThatThrownBy(() -> this.notificationPublisherV1.publish(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Outbox event and payload are required");
        verifyNoInteractions(event);
    }

    @Test
    void givenEvent_whenPublish_thenSendEnvelope() {
        //given
        final EmailVerifyPayload payload = mock(EmailVerifyPayload.class);
        final Event<EmailVerifyPayload> event = mock(Event.class);
        final NotificationEnvelope envelope = mock(NotificationEnvelope.class);
        final UUID idempotencyId = UUID.fromString("70ef4ab8-6728-495d-8922-3b7eeb3af05c");
        final Instant createdAt = Instant.parse("2026-03-02T09:00:00Z");
        final String eventType = "EMAIL_VERIFY";

        when(event.getPayload())
                .thenReturn(payload);
        when(event.getIdempotencyId())
                .thenReturn(idempotencyId);
        when(event.getCreatedAt())
                .thenReturn(createdAt);
        when(event.getEventType())
                .thenReturn(eventType);
        when(this.mapper.asEnvelope(event))
                .thenReturn(envelope);

        //when
        this.notificationPublisherV1.publish(event);

        //then
        verify(this.mapper).asEnvelope(event);
        verify(event).getPayload();
        verify(event).getIdempotencyId();
        verify(event).getCreatedAt();
        verify(event).getEventType();
        verify(this.producer).send(idempotencyId.toString(), envelope);
        verifyNoMoreInteractions(payload, event, envelope);
    }
}
