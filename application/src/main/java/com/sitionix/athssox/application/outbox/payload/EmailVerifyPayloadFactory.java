package com.sitionix.athssox.application.outbox.payload;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenIssue;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class EmailVerifyPayloadFactory {

    public EmailVerifyPayload create(final Long userId,
                                     final UUID siteId,
                                     final String email,
                                     final String traceId,
                                     final Instant requestedAt,
                                     final EmailVerificationTokenIssue tokenIssue) {
        Objects.requireNonNull(tokenIssue, "tokenIssue is required");

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
