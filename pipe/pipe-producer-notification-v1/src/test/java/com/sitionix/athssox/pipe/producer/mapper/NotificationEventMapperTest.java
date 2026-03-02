package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.events.Metadata;
import com.app_afesox.ntfssox.events.notifications.DeliveryDTO;
import com.app_afesox.ntfssox.events.notifications.MetaDTO;
import com.app_afesox.ntfssox.events.notifications.NotificationChannelDTO;
import com.app_afesox.ntfssox.events.notifications.NotificationEvent;
import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.app_afesox.ntfssox.events.notifications.NotificationTemplateDTO;
import com.app_afesox.ntfssox.events.notifications.contents.EmailVerificationContentDTO;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.EventMetadataContract;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventMapperTest {

    private NotificationEventMapper notificationEventMapper;

    @Mock
    private NotificationContentEventMapper contentMapper;

    @Mock
    private NotificationDeliveryEventMapper deliveryMapper;

    @Mock
    private NotificationMetaEventMapper metaMapper;

    @Mock
    private NotificationTemplateEventMapper templateMapper;

    @BeforeEach
    void setUp() {
        this.notificationEventMapper = new NotificationEventMapperImpl(this.contentMapper,
                this.deliveryMapper,
                this.metaMapper,
                this.templateMapper);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.contentMapper,
                this.deliveryMapper,
                this.metaMapper,
                this.templateMapper);
    }

    @Test
    void givenEmailVerifyPayload_whenAsEvent_thenReturnNotificationEvent() {
        //given
        final UUID siteId = this.getSiteId();
        final Instant requestedAt = this.getInstant("2024-04-22T08:15:30Z");

        final EmailVerifyPayload given = this.getEmailVerifyPayload(siteId, requestedAt);
        final EmailVerificationContentDTO content = this.getContent();
        final DeliveryDTO delivery = this.getDelivery();
        final MetaDTO meta = this.getMeta(siteId, requestedAt);
        final NotificationTemplateDTO template = this.getTemplate();

        when(this.contentMapper.asContent(given.getParams()))
                .thenReturn(content);
        when(this.deliveryMapper.asDelivery(given.getDelivery()))
                .thenReturn(delivery);
        when(this.metaMapper.asMeta(given.getMeta()))
                .thenReturn(meta);
        when(this.templateMapper.asTemplate(given.getTemplate()))
                .thenReturn(template);

        final NotificationEvent expected = this.getNotificationEvent(delivery, template, content, meta);

        //when
        final NotificationEvent actual = this.notificationEventMapper.asEvent(given);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.contentMapper)
                .asContent(given.getParams());
        verify(this.deliveryMapper)
                .asDelivery(given.getDelivery());
        verify(this.metaMapper)
                .asMeta(given.getMeta());
        verify(this.templateMapper)
                .asTemplate(given.getTemplate());
    }

    @Test
    void givenPayloadAndMetadata_whenAsEnvelope_thenReturnNotificationEventEnvelope() {
        //given
        final UUID siteId = this.getSiteId();
        final Instant requestedAt = this.getInstant("2024-04-22T08:15:30Z");
        final Instant createdAt = this.getInstant("2024-04-22T08:16:30Z");
        final UUID idempotencyId = UUID.fromString("f899ee7f-6e45-4967-ac17-6c13c7ae5e0f");

        final EmailVerifyPayload payload = this.getEmailVerifyPayload(siteId, requestedAt);
        final EventMetadataContract metadata = this.getPublishMetadata(idempotencyId, createdAt);

        final EmailVerificationContentDTO content = this.getContent();
        final DeliveryDTO delivery = this.getDelivery();
        final MetaDTO meta = this.getMeta(siteId, requestedAt);
        final NotificationTemplateDTO template = this.getTemplate();

        when(this.contentMapper.asContent(payload.getParams()))
                .thenReturn(content);
        when(this.deliveryMapper.asDelivery(payload.getDelivery()))
                .thenReturn(delivery);
        when(this.metaMapper.asMeta(payload.getMeta()))
                .thenReturn(meta);
        when(this.templateMapper.asTemplate(payload.getTemplate()))
                .thenReturn(template);

        final NotificationEvent expectedPayload = this.getNotificationEvent(delivery, template, content, meta);
        final Metadata expectedMetadata = this.getMetadata(idempotencyId,
                createdAt,
                NotificationTemplate.EMAIL_VERIFY.getDescription());
        final NotificationEnvelope expected = this.getNotificationEnvelope(expectedMetadata, expectedPayload);

        //when
        final NotificationEnvelope actual = this.notificationEventMapper.asEnvelope(payload, metadata);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.contentMapper)
                .asContent(payload.getParams());
        verify(this.deliveryMapper)
                .asDelivery(payload.getDelivery());
        verify(this.metaMapper)
                .asMeta(payload.getMeta());
        verify(this.templateMapper)
                .asTemplate(payload.getTemplate());
    }

    @Test
    void givenPublishMetadata_whenAsMetadata_thenReturnMetadata() {
        //given
        final Instant createdAt = this.getInstant("2024-04-23T08:16:30Z");
        final UUID idempotencyId = UUID.fromString("2b2077f5-987f-43a2-af1b-463154649ffb");
        final EventMetadataContract given = this.getPublishMetadata(idempotencyId, createdAt);
        final Metadata expected = this.getMetadata(idempotencyId,
                createdAt,
                NotificationTemplate.EMAIL_VERIFY.getDescription());

        //when
        final Metadata actual = this.notificationEventMapper.asMetadata(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenInstant_whenToDateTime_thenReturnDateTimeString() {
        //given
        final Instant given = this.getInstant("2024-04-24T08:15:30Z");
        final String expected = given.toString();

        //when
        final String actual = this.notificationEventMapper.toDateTime(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenInstant_whenToEpochMillis_thenReturnEpochMillis() {
        //given
        final Instant given = this.getInstant("2024-04-25T08:15:30Z");
        final Long expected = given.toEpochMilli();

        //when
        final Long actual = this.notificationEventMapper.toEpochMillis(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenNullInstant_whenToDateTime_thenReturnNull() {
        //given
        final Instant given = null;

        //when
        final String actual = this.notificationEventMapper.toDateTime(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenNullInstant_whenToEpochMillis_thenReturnNull() {
        //given
        final Instant given = null;

        //when
        final Long actual = this.notificationEventMapper.toEpochMillis(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenNullUuid_whenToString_thenReturnNull() {
        //given
        final UUID given = null;

        //when
        final String actual = this.notificationEventMapper.toString(given);

        //then
        assertThat(actual).isNull();
    }

    private EmailVerifyPayload getEmailVerifyPayload(final UUID siteId,
                                                     final Instant requestedAt) {
        return EmailVerifyPayload.builder()
                .delivery(this.getEmailVerifyPayloadDelivery())
                .template(NotificationTemplate.EMAIL_VERIFY)
                .params(this.getEmailVerifyPayloadParams())
                .meta(this.getEmailVerifyPayloadMeta(siteId, requestedAt))
                .build();
    }

    private EmailVerifyPayload.Delivery getEmailVerifyPayloadDelivery() {
        return EmailVerifyPayload.Delivery.builder()
                .channel(VerifyChannel.EMAIL)
                .to("user@sitionix.com")
                .build();
    }

    private EmailVerifyPayload.Params getEmailVerifyPayloadParams() {
        return EmailVerifyPayload.Params.builder()
                .emailVerificationTokenId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .pepperId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .build();
    }

    private EmailVerifyPayload.Meta getEmailVerifyPayloadMeta(final UUID siteId,
                                                              final Instant requestedAt) {
        return EmailVerifyPayload.Meta.builder()
                .userId(42L)
                .siteId(siteId)
                .traceId("trace-123")
                .requestedAt(requestedAt)
                .build();
    }

    private NotificationEvent getNotificationEvent(final DeliveryDTO delivery,
                                                   final NotificationTemplateDTO template,
                                                   final EmailVerificationContentDTO content,
                                                   final MetaDTO meta) {
        return NotificationEvent.newBuilder()
                .setDelivery(delivery)
                .setTemplate(template)
                .setContent(content)
                .setMeta(meta)
                .build();
    }

    private DeliveryDTO getDelivery() {
        return DeliveryDTO.newBuilder()
                .setChannel(NotificationChannelDTO.EMAIL)
                .setTo("user@sitionix.com")
                .build();
    }

    private EmailVerificationContentDTO getContent() {
        return EmailVerificationContentDTO.newBuilder()
                .setVerificationTokenId("11111111-1111-1111-1111-111111111111")
                .setPepperId("22222222-2222-2222-2222-222222222222")
                .build();
    }

    private MetaDTO getMeta(final UUID siteId,
                            final Instant requestedAt) {
        return MetaDTO.newBuilder()
                .setUserId(42L)
                .setSiteId(siteId.toString())
                .setTraceId("trace-123")
                .setRequestedAt(requestedAt.toString())
                .build();
    }

    private NotificationTemplateDTO getTemplate() {
        return NotificationTemplateDTO.EMAIL_VERIFY;
    }

    private UUID getSiteId() {
        return UUID.fromString("9cdeca1b-0580-4b75-9b93-e5b7f786b3c0");
    }

    private Instant getInstant(final String value) {
        return Instant.parse(value);
    }

    private EventMetadataContract getPublishMetadata(final UUID idempotencyId,
                                                     final Instant createdAt) {
        return new EventMetadataContract() {
            @Override
            public UUID getIdempotencyId() {
                return idempotencyId;
            }

            @Override
            public Instant getCreatedAt() {
                return createdAt;
            }

            @Override
            public String getEventType() {
                return NotificationTemplate.EMAIL_VERIFY.getDescription();
            }
        };
    }

    private Metadata getMetadata(final UUID idempotencyId,
                                 final Instant createdAt,
                                 final String eventType) {
        return Metadata.newBuilder()
                .setIdempotencyId(idempotencyId.toString())
                .setCreatedAt(createdAt.toEpochMilli())
                .setEventType(eventType)
                .build();
    }

    private NotificationEnvelope getNotificationEnvelope(final Metadata metadata,
                                                         final NotificationEvent payload) {
        return NotificationEnvelope.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }
}
