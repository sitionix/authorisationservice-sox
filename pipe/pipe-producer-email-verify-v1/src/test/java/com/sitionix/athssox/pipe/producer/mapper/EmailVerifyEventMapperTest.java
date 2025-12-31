package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.athssox.events.emailverify.Delivery;
import com.app_afesox.athssox.events.emailverify.EmailVerifyEvent;
import com.app_afesox.athssox.events.emailverify.EmailVerifyEventEnvelope;
import com.app_afesox.athssox.events.emailverify.Meta;
import com.app_afesox.athssox.events.emailverify.Params;
import com.app_afesox.events.Metadata;
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

import static com.app_afesox.athssox.events.emailverify.OutboxEventType.EMAIL_VERIFY;
import static com.app_afesox.athssox.events.emailverify.VerifyChannel.EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailVerifyEventMapperTest {

    private EmailVerifyEventMapper emailVerifyEventMapper;

    @BeforeEach
    void setUp() {
        this.emailVerifyEventMapper = new EmailVerifyEventMapperImpl();
    }

    @Test
    void given_email_verify_payload_when_as_event_then_return_email_verify_event() {
        //given
        final UUID siteId = UUID.randomUUID();
        final Instant requestedAt = Instant.now();

        final EmailVerifyPayload given = this.getEmailVerifyPayload(siteId, requestedAt);
        final EmailVerifyEvent expected = this.getEmailVerifyEvent(siteId, requestedAt);

        //when
        final EmailVerifyEvent actual = this.emailVerifyEventMapper.asEvent(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_event_when_as_envelope_then_return_email_verify_event_envelope() {
        //given
        final UUID siteId = UUID.randomUUID();
        final Instant requestedAt = Instant.parse("2024-04-22T08:15:30Z");
        final Instant createdAt = Instant.parse("2024-04-22T08:16:30Z");

        final EmailVerifyPayload payload = this.getEmailVerifyPayload(siteId, requestedAt);
        final Event<EmailVerifyPayload> given = this.getEvent(payload, createdAt);

        final EmailVerifyEvent expectedPayload = this.getEmailVerifyEvent(siteId, requestedAt);
        final Metadata expectedMetadata = this.getMetadata(given.getIdempotencyId(),
                createdAt,
                given.getEventType());
        final EmailVerifyEventEnvelope expected = this.getEmailVerifyEventEnvelope(expectedMetadata, expectedPayload);

        //when
        final EmailVerifyEventEnvelope actual = this.emailVerifyEventMapper.asEnvelope(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_event_when_as_metadata_then_return_metadata() {
        //given
        final UUID siteId = UUID.randomUUID();
        final Instant requestedAt = Instant.parse("2024-04-23T08:15:30Z");
        final Instant createdAt = Instant.parse("2024-04-23T08:16:30Z");

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
    void given_instant_when_to_date_time_then_return_date_time_string() {
        //given
        final Instant given = Instant.parse("2024-04-24T08:15:30Z");
        final String expected = given.toString();

        //when
        final String actual = this.emailVerifyEventMapper.toDateTime(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_instant_when_to_epoch_millis_then_return_epoch_millis() {
        //given
        final Instant given = Instant.parse("2024-04-25T08:15:30Z");
        final Long expected = given.toEpochMilli();

        //when
        final Long actual = this.emailVerifyEventMapper.toEpochMillis(given);

        //then
        assertThat(actual).isEqualTo(expected);
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
                .verifyUrl("https://verify.sitionix.com/token")
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

    private EmailVerifyEvent getEmailVerifyEvent(final UUID siteId,
                                                 final Instant requestedAt) {
        return EmailVerifyEvent.newBuilder()
                .setDelivery(this.getDelivery())
                .setTemplate(EMAIL_VERIFY)
                .setParams(this.getParams())
                .setMeta(this.getMeta(siteId, requestedAt))
                .build();
    }

    private Delivery getDelivery() {
        return Delivery.newBuilder()
                .setChannel(EMAIL)
                .setTo("user@sitionix.com")
                .build();
    }

    private Params getParams() {
        return Params.newBuilder()
                .setVerifyUrl("https://verify.sitionix.com/token")
                .build();
    }

    private Meta getMeta(final UUID siteId,
                         final Instant requestedAt) {
        return Meta.newBuilder()
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

    private Metadata getMetadata(final UUID idempotencyId,
                                 final Instant createdAt,
                                 final String eventType) {
        return Metadata.newBuilder()
                .setIdempotencyId(idempotencyId.toString())
                .setCreatedAt(createdAt.toEpochMilli())
                .setEventType(eventType)
                .build();
    }

    private EmailVerifyEventEnvelope getEmailVerifyEventEnvelope(final Metadata metadata,
                                                                 final EmailVerifyEvent payload) {
        return EmailVerifyEventEnvelope.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();
    }
}
