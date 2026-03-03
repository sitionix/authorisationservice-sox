package com.sitionix.athssox.application.outbox.payload;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenIssue;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailVerifyPayloadFactoryTest {

    private EmailVerifyPayloadFactory emailVerifyPayloadFactory;

    @BeforeEach
    void setUp() {
        this.emailVerifyPayloadFactory = new EmailVerifyPayloadFactory();
    }

    @Test
    void givenValidInput_whenCreate_thenReturnMappedPayload() {
        //given
        final Long userId = 10L;
        final UUID siteId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        final String email = "user@sitionix.com";
        final String traceId = "trace-1";
        final Instant requestedAt = Instant.parse("2026-01-01T10:00:00Z");
        final EmailVerificationTokenIssue tokenIssue = this.getTokenIssue();
        final EmailVerifyPayload expected = this.getEmailVerifyPayload(userId, siteId, email, traceId, requestedAt, tokenIssue);

        //when
        final EmailVerifyPayload actual = this.emailVerifyPayloadFactory.create(
                userId,
                siteId,
                email,
                traceId,
                requestedAt,
                tokenIssue);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenNullTokenIssue_whenCreate_thenThrowException() {
        //given
        final Long userId = 10L;
        final UUID siteId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        final String email = "user@sitionix.com";
        final String traceId = "trace-1";
        final Instant requestedAt = Instant.parse("2026-01-01T10:00:00Z");

        //when
        //then
        assertThatThrownBy(() -> this.emailVerifyPayloadFactory.create(
                userId,
                siteId,
                email,
                traceId,
                requestedAt,
                null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("tokenIssue is required");
    }

    private EmailVerificationTokenIssue getTokenIssue() {
        return new EmailVerificationTokenIssue(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"));
    }

    private EmailVerifyPayload getEmailVerifyPayload(final Long userId,
                                                     final UUID siteId,
                                                     final String email,
                                                     final String traceId,
                                                     final Instant requestedAt,
                                                     final EmailVerificationTokenIssue tokenIssue) {
        return EmailVerifyPayload.builder()
                .delivery(EmailVerifyPayload.Delivery.builder()
                        .channel(VerifyChannel.EMAIL)
                        .to(email)
                        .build())
                .template(NotificationTemplate.EMAIL_VERIFY)
                .params(EmailVerifyPayload.Params.builder()
                        .emailVerificationTokenId(tokenIssue.tokenId())
                        .pepperId(tokenIssue.pepperId())
                        .build())
                .meta(EmailVerifyPayload.Meta.builder()
                        .userId(userId)
                        .siteId(siteId)
                        .traceId(traceId)
                        .requestedAt(requestedAt)
                        .build())
                .build();
    }
}
