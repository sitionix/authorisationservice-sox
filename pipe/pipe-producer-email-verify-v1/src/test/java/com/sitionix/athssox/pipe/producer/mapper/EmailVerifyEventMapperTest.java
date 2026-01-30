package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.events.Metadata;
import com.app_afesox.ntfssox.events.notifications.DeliveryDTO;
import com.app_afesox.ntfssox.events.notifications.MetaDTO;
import com.app_afesox.ntfssox.events.notifications.NotificationChannelDTO;
import com.app_afesox.ntfssox.events.notifications.NotificationEvent;
import com.app_afesox.ntfssox.events.notifications.NotificationEnvelope;
import com.app_afesox.ntfssox.events.notifications.NotificationTemplateDTO;
import com.app_afesox.ntfssox.events.notifications.contents.EmailVerificationContentDTO;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailVerifyEventMapperTest {

    private EmailVerifyEventMapper emailVerifyEventMapper;

    @BeforeEach
    void setUp() {
        final EmailVerifyContentEventMapper contentMapper = new EmailVerifyContentEventMapperImpl();
        final EmailVerifyDeliveryEventMapper deliveryMapper = new EmailVerifyDeliveryEventMapperImpl();
        final EmailVerifyMetaEventMapper metaMapper = new EmailVerifyMetaEventMapperImpl();
        final EmailVerifyTemplateEventMapper templateMapper = new EmailVerifyTemplateEventMapperImpl();

        this.emailVerifyEventMapper = new EmailVerifyEventMapperImpl(contentMapper,
                deliveryMapper,
                metaMapper,
                templateMapper);
    }

    @Test
    void givenEmailVerifyPayload_whenAsEvent_thenReturnNotificationEvent() {
        //given
        final UUID siteId = this.getSiteId();
        final Instant requestedAt = this.getInstant("2024-04-22T08:15:30Z");

        final EmailVerifyPayload given = this.getEmailVerifyPayload(siteId, requestedAt);
        final NotificationEvent expected = this.getNotificationEvent(siteId, requestedAt);

        //when
        final NotificationEvent actual = this.emailVerifyEventMapper.asEvent(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenEvent_whenAsEnvelope_thenReturnNotificationEventEnvelope() {
        //given
        final UUID siteId = this.getSiteId();
        final Instant requestedAt = this.getInstant("2024-04-22T08:15:30Z");
        final Instant createdAt = this.getInstant("2024-04-22T08:16:30Z");

        final EmailVerifyPayload payload = this.getEmailVerifyPayload(siteId, requestedAt);
        final Event<EmailVerifyPayload> given = this.getEvent(payload, createdAt);

        final NotificationEvent expectedPayload = this.getNotificationEvent(siteId, requestedAt);
        final Metadata expectedMetadata = this.getMetadata(given.getIdempotencyId(),
                createdAt,
                given.getEventType());
        final NotificationEnvelope expected = this.getNotificationEnvelope(expectedMetadata, expectedPayload);

        //when
        final NotificationEnvelope actual = this.emailVerifyEventMapper.asEnvelope(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenEvent_whenAsMetadata_thenReturnMetadata() {
        //given
        final UUID siteId = this.getSiteId();
        final Instant requestedAt = this.getInstant("2024-04-23T08:15:30Z");
        final Instant createdAt = this.getInstant("2024-04-23T08:16:30Z");

        final EmailVerifyPayload payload = this.getEmailVerifyPayload(siteId, requestedAt);
        final Event<EmailVerifyPayload> given = this.getEvent(payload, createdAt);
        final Metadata expected = this.getMetadata(given.getIdempotencyId(),
                createdAt,
                given.getEventType());

        //when
        final Metadata actual = this.emailVerifyEventMapper.asMetadata(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenInstant_whenToDateTime_thenReturnDateTimeString() {
        //given
        final Instant given = this.getInstant("2024-04-24T08:15:30Z");
        final String expected = given.toString();

        //when
        final String actual = this.emailVerifyEventMapper.toDateTime(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenInstant_whenToEpochMillis_thenReturnEpochMillis() {
        //given
        final Instant given = this.getInstant("2024-04-25T08:15:30Z");
        final Long expected = given.toEpochMilli();

        //when
        final Long actual = this.emailVerifyEventMapper.toEpochMillis(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenNullInstant_whenToDateTime_thenReturnNull() {
        //given
        final Instant given = null;

        //when
        final String actual = this.emailVerifyEventMapper.toDateTime(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenNullInstant_whenToEpochMillis_thenReturnNull() {
        //given
        final Instant given = null;

        //when
        final Long actual = this.emailVerifyEventMapper.toEpochMillis(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenNullUuid_whenToString_thenReturnNull() {
        //given
        final UUID given = null;

        //when
        final String actual = this.emailVerifyEventMapper.toString(given);

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

    private NotificationEvent getNotificationEvent(final UUID siteId,
                                                   final Instant requestedAt) {
        return NotificationEvent.newBuilder()
                .setDelivery(this.getDelivery())
                .setTemplate(NotificationTemplateDTO.EMAIL_VERIFY)
                .setContent(this.getContent())
                .setMeta(this.getMeta(siteId, requestedAt))
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

    private Event<EmailVerifyPayload> getEvent(final EmailVerifyPayload payload,
                                               final Instant createdAt) {
        return Event.create(this.getOutboxEvent(payload, createdAt));
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

    private UUID getSiteId() {
        return UUID.fromString("9cdeca1b-0580-4b75-9b93-e5b7f786b3c0");
    }

    private Instant getInstant(final String value) {
        return Instant.parse(value);
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
