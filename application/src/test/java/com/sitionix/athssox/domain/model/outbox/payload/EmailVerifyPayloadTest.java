package com.sitionix.athssox.domain.model.outbox.payload;

import com.sitionix.forge.outbox.core.model.OutboxAggregateType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerifyPayloadTest {

    @Test
    void givenPayloadWithTokenAndMeta_whenResolveMetadataContract_thenReturnStableValues() {
        //given
        final UUID tokenId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        final UUID pepperId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        final Instant requestedAt = Instant.parse("2026-01-01T10:00:00Z");
        final EmailVerifyPayload payload = this.getPayload(tokenId, pepperId, requestedAt, "trace-1");

        //when
        final UUID idempotencyId = payload.getIdempotencyId();
        final Instant createdAt = payload.getCreatedAt();
        final String eventType = payload.getEventType();
        final OutboxAggregateType aggregateType = payload.getAgregateType();
        final Long aggregateId = payload.getAgregateId();
        final String traceId = payload.getOutboxTraceId();

        //then
        assertThat(idempotencyId).isEqualTo(tokenId);
        assertThat(createdAt).isEqualTo(requestedAt);
        assertThat(eventType).isEqualTo("EMAIL_VERIFY");
        assertThat(aggregateType).isEqualTo(OutboxAggregateType.USER);
        assertThat(aggregateId).isEqualTo(10L);
        assertThat(traceId).isEqualTo("trace-1");
    }

    @Test
    void givenPayloadWithoutToken_whenResolveIdempotencyTwice_thenReturnSameValue() {
        //given
        final EmailVerifyPayload payload = this.getPayload(null,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-01-01T10:00:00Z"),
                null);

        //when
        final UUID first = payload.getIdempotencyId();
        final UUID second = payload.getIdempotencyId();

        //then
        assertThat(first).isEqualTo(second);
        assertThat(first).isNotNull();
    }

    @Test
    void givenPayloadWithoutTemplateAndMeta_whenResolveEventTypeAndTrace_thenReturnDefaults() {
        //given
        final EmailVerifyPayload payload = EmailVerifyPayload.builder()
                .delivery(EmailVerifyPayload.Delivery.builder()
                        .channel(VerifyChannel.EMAIL)
                        .to("user@sitionix.com")
                        .build())
                .params(EmailVerifyPayload.Params.builder()
                        .emailVerificationTokenId(null)
                        .pepperId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                        .build())
                .meta(null)
                .template(null)
                .build();

        //when
        final String eventType = payload.getEventType();
        final String traceId = payload.getOutboxTraceId();

        //then
        assertThat(eventType).isEqualTo("EMAIL_VERIFY");
        assertThat(traceId).isNull();
    }

    private EmailVerifyPayload getPayload(final UUID tokenId,
                                          final UUID pepperId,
                                          final Instant requestedAt,
                                          final String traceId) {
        return EmailVerifyPayload.builder()
                .delivery(EmailVerifyPayload.Delivery.builder()
                        .channel(VerifyChannel.EMAIL)
                        .to("user@sitionix.com")
                        .build())
                .template(NotificationTemplate.EMAIL_VERIFY)
                .params(EmailVerifyPayload.Params.builder()
                        .emailVerificationTokenId(tokenId)
                        .pepperId(pepperId)
                        .build())
                .meta(EmailVerifyPayload.Meta.builder()
                        .userId(10L)
                        .siteId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                        .traceId(traceId)
                        .requestedAt(requestedAt)
                        .build())
                .build();
    }
}
