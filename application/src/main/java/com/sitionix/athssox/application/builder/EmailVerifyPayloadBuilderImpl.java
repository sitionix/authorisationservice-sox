package com.sitionix.athssox.application.builder;

import com.sitionix.athssox.domain.builder.EmailVerifyPayloadBuilder;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import com.sitionix.athssox.domain.model.emailverify.EmailVerifyPayloadContext;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenIssue;
import com.sitionix.athssox.domain.service.EmailVerificationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public final class EmailVerifyPayloadBuilderImpl implements EmailVerifyPayloadBuilder {

    private final EmailVerificationTokenService tokenService;

    @Override
    public EmailVerifyPayload build(final EmailVerifyPayloadContext ctx) {
        final EmailVerificationTokenIssue tokenIssue = this.tokenService.issue(ctx.userId(), ctx.siteId());
        return this.buildPayload(ctx, tokenIssue.tokenId(), tokenIssue.pepperId());
    }

    private EmailVerifyPayload buildPayload(final EmailVerifyPayloadContext ctx,
                                            final UUID verificationTokenId,
                                            final UUID pepperId) {
        return EmailVerifyPayload.builder()
                .delivery(EmailVerifyPayload.Delivery.builder()
                        .channel(VerifyChannel.EMAIL)
                        .to(ctx.email())
                        .build())
                .template(NotificationTemplate.EMAIL_VERIFY)
                .params(EmailVerifyPayload.Params.builder()
                        .emailVerificationTokenId(verificationTokenId)
                        .pepperId(pepperId)
                        .build())
                .meta(EmailVerifyPayload.Meta.builder()
                        .userId(ctx.userId())
                        .siteId(ctx.siteId())
                        .traceId(ctx.traceId())
                        .requestedAt(ctx.requestedAt())
                        .build())
                .build();
    }
}
