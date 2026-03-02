package com.sitionix.athssox.application.builder;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenIssue;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import com.sitionix.athssox.domain.service.EmailVerificationTokenService;
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
class EmailVerifyPayloadBuilderImplTest {

    private EmailVerifyPayloadBuilderImpl builder;

    @Mock
    private EmailVerificationTokenService tokenService;

    @BeforeEach
    void setUp() {
        this.builder = new EmailVerifyPayloadBuilderImpl(this.tokenService);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.tokenService);
    }

    @Test
    void givenValues_whenBuild_thenReturnEmailVerifyPayload() {
        //given
        final Long userId = 1L;
        final UUID siteId = this.getSiteId();
        final Instant requestedAt = this.getRequestedAt();
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();
        final String email = "email";
        final String traceId = "traceId";

        when(this.tokenService.issue(userId, siteId))
                .thenReturn(this.getTokenIssue(tokenId, pepperId));

        //when
        final EmailVerifyPayload actual = this.builder.build(userId, siteId, email, traceId, requestedAt);

        //then
        final EmailVerifyPayload expected = this.getEmailVerifyPayload(siteId, requestedAt, tokenId, pepperId);

        assertThat(actual).isEqualTo(expected);
        assertThat(actual.getIdempotencyId()).isEqualTo(tokenId);
        assertThat(actual.getCreatedAt()).isEqualTo(requestedAt);
        assertThat(actual.getEventType()).isEqualTo(NotificationTemplate.EMAIL_VERIFY.getDescription());

        verify(this.tokenService)
                .issue(userId, siteId);
    }

    private UUID getSiteId() {
        return UUID.fromString("8f24d9f6-2c05-4b77-8c4e-1bc6e1ba9b6c");
    }

    private Instant getRequestedAt() {
        return Instant.parse("2024-05-01T10:15:30Z");
    }

    private UUID getTokenId() {
        return UUID.fromString("c9b1f3f4-12c7-11ec-82a8-0242ac130003");
    }

    private UUID getPepperId() {
        return UUID.fromString("2cf629c1-1b58-4aa3-a9fd-5e9be2b1d31d");
    }

    private EmailVerifyPayload getEmailVerifyPayload(final UUID siteId,
                                                     final Instant requestedAt,
                                                     final UUID tokenId,
                                                     final UUID pepperId) {
        return EmailVerifyPayload.builder()
                .delivery(this.getDelivery())
                .template(NotificationTemplate.EMAIL_VERIFY)
                .params(this.getParams(tokenId, pepperId))
                .meta(this.getMeta(siteId, requestedAt))
                .idempotencyId(tokenId)
                .createdAt(requestedAt)
                .build();
    }

    private EmailVerifyPayload.Delivery getDelivery() {
        return EmailVerifyPayload.Delivery.builder()
                .channel(VerifyChannel.EMAIL)
                .to("email")
                .build();
    }

    private EmailVerifyPayload.Params getParams(final UUID tokenId, final UUID pepperId) {
        return EmailVerifyPayload.Params.builder()
                .emailVerificationTokenId(tokenId)
                .pepperId(pepperId)
                .build();
    }

    private EmailVerificationTokenIssue getTokenIssue(final UUID tokenId, final UUID pepperId) {
        return new EmailVerificationTokenIssue(tokenId, pepperId);
    }

    private EmailVerifyPayload.Meta getMeta(final UUID siteId, final Instant instant) {
        return EmailVerifyPayload.Meta.builder()
                .userId(1L)
                .siteId(siteId)
                .traceId("traceId")
                .requestedAt(instant)
                .build();
    }

}
