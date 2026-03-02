package com.sitionix.athssox.pipe.producer;

import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.app_afesox.ntfssox.events.notifications.kafka.NotificationsV1Producer;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.EventMetadataContract;
import com.sitionix.athssox.pipe.producer.mapper.NotificationEventMapper;
import com.sitionix.forge.outbox.core.port.ForgeOutboxPublishMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
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
        final ForgeOutboxPublishMetadata metadata = mock(ForgeOutboxPublishMetadata.class);

        //when
        //then
        assertThatThrownBy(() -> this.notificationPublisherV1.publish(null, metadata))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Outbox payload and metadata are required");
        verifyNoInteractions(metadata);
    }

    @Test
    void givenPayloadAndMetadata_whenPublish_thenSendEnvelope() {
        //given
        final EmailVerifyPayload payload = mock(EmailVerifyPayload.class);
        final ForgeOutboxPublishMetadata metadata = mock(ForgeOutboxPublishMetadata.class);
        final NotificationEnvelope envelope = mock(NotificationEnvelope.class);
        final UUID idempotencyId = UUID.fromString("70ef4ab8-6728-495d-8922-3b7eeb3af05c");
        final Instant createdAt = Instant.parse("2026-03-02T09:00:00Z");
        final String eventType = "EMAIL_VERIFY";

        when(metadata.getIdempotencyId())
                .thenReturn(idempotencyId);
        when(metadata.getCreatedAt())
                .thenReturn(createdAt);
        when(metadata.getEventType())
                .thenReturn(eventType);
        when(this.mapper.asEnvelope(eq(payload), any(EventMetadataContract.class)))
                .thenReturn(envelope);

        //when
        this.notificationPublisherV1.publish(payload, metadata);

        //then
        final ArgumentCaptor<EventMetadataContract> metadataCaptor = ArgumentCaptor.forClass(EventMetadataContract.class);
        verify(this.mapper).asEnvelope(eq(payload), metadataCaptor.capture());
        assertThat(metadataCaptor.getValue().getIdempotencyId()).isEqualTo(idempotencyId);
        assertThat(metadataCaptor.getValue().getCreatedAt()).isEqualTo(createdAt);
        assertThat(metadataCaptor.getValue().getEventType()).isEqualTo(eventType);
        verify(metadata).getIdempotencyId();
        verify(metadata).getCreatedAt();
        verify(metadata).getEventType();
        verify(this.producer).send(idempotencyId.toString(), envelope);
        verifyNoMoreInteractions(payload, metadata, envelope);
    }
}
