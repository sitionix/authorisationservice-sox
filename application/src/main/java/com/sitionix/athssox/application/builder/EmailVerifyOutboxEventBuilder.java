package com.sitionix.athssox.application.builder;

import com.sitionix.athssox.domain.builder.OutboxEventBuilder;
import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxBuildContext;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.InitiatorType;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenIssue;
import com.sitionix.athssox.domain.service.EmailVerificationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public final class EmailVerifyOutboxEventBuilder implements OutboxEventBuilder<EmailVerifyPayload> {

    private final EmailVerificationTokenService tokenService;

    @Override
    public OutboxEventType eventType() {
        return OutboxEventType.EMAIL_VERIFY;
    }

    @Override
    public OutboxEvent<EmailVerifyPayload> build(final OutboxBuildContext ctx) {
        final EmailVerificationTokenIssue tokenIssue = this.tokenService.issue(ctx.userId(), ctx.siteId());
        final EmailVerifyPayload payload = this.buildPayload(ctx, tokenIssue.tokenId());

        return OutboxEvent.<EmailVerifyPayload>builder()
                .aggregateType(OutboxAggregateType.USER)
                .aggregateId(ctx.userId())
                .initiatorType(InitiatorType.USER)
                .initiatorId(ctx.userId().toString())
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now())
                .payload(payload)
                .lastError(null)
                .build();
    }

    private EmailVerifyPayload buildPayload(final OutboxBuildContext ctx, final java.util.UUID verificationTokenId) {
        return EmailVerifyPayload.builder()
                .delivery(EmailVerifyPayload.Delivery.builder()
                        .channel(VerifyChannel.EMAIL)
                        .to(ctx.email())
                        .build())
                .template(NotificationTemplate.EMAIL_VERIFY)
                .params(EmailVerifyPayload.Params.builder()
                        .verificationTokenId(verificationTokenId)
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
