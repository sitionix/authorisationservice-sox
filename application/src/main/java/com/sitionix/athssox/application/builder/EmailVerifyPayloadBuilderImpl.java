package com.sitionix.athssox.application.builder;

import com.sitionix.athssox.domain.builder.EmailVerifyPayloadBuilder;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenIssue;
import com.sitionix.athssox.domain.service.EmailVerificationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public final class EmailVerifyPayloadBuilderImpl implements EmailVerifyPayloadBuilder {

    private final EmailVerificationTokenService tokenService;

    @Override
    public EmailVerifyPayload build(final Long userId,
                                    final UUID siteId,
                                    final String email,
                                    final String traceId,
                                    final Instant requestedAt) {
        final EmailVerificationTokenIssue tokenIssue = this.tokenService.issue(userId, siteId);
        return this.buildPayload(userId, siteId, email, traceId, requestedAt, tokenIssue.tokenId(), tokenIssue.pepperId());
    }

    private EmailVerifyPayload buildPayload(final Long userId,
                                            final UUID siteId,
                                            final String email,
                                            final String traceId,
                                            final Instant requestedAt,
                                            final UUID verificationTokenId,
                                            final UUID pepperId) {
        return EmailVerifyPayload.builder()
                .delivery(EmailVerifyPayload.Delivery.builder()
                        .channel(VerifyChannel.EMAIL)
                        .to(email)
                        .build())
                .template(NotificationTemplate.EMAIL_VERIFY)
                .params(EmailVerifyPayload.Params.builder()
                        .emailVerificationTokenId(verificationTokenId)
                        .pepperId(pepperId)
                        .build())
                .meta(EmailVerifyPayload.Meta.builder()
                        .userId(userId)
                        .siteId(siteId)
                        .traceId(traceId)
                        .requestedAt(requestedAt)
                        .build())
                .idempotencyId(verificationTokenId)
                .createdAt(requestedAt)
                .build();
    }
}
