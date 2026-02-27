package com.sitionix.athssox.pipe.producer;

import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.app_afesox.ntfssox.events.notifications.kafka.NotificationsV1Producer;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.pipe.producer.mapper.NotificationEventMapper;
import com.sitionix.forge.outbox.core.port.ForgeOutboxPublishMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        when(metadata.getIdempotencyId())
                .thenReturn(idempotencyId);
        when(this.mapper.asEnvelope(payload, metadata))
                .thenReturn(envelope);

        //when
        this.notificationPublisherV1.publish(payload, metadata);

        //then
        verify(this.mapper).asEnvelope(payload, metadata);
        verify(metadata).getIdempotencyId();
        verify(this.producer).send(idempotencyId.toString(), envelope);
        verifyNoMoreInteractions(payload, metadata, envelope);
    }
}
