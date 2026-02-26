package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.domain.builder.EmailVerifyPayloadBuilder;
import com.sitionix.athssox.domain.exception.EmailVerificationResendNotAllowedException;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.ResendEmailVerificationResponse;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.model.emailverify.EmailVerifyPayloadContext;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.repository.AuthUserRepository;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.service.EmailVerificationResendPolicy;
import com.sitionix.athssox.domain.usecase.ResendEmailVerification;
import com.sitionix.forge.outbox.core.command.ForgeOutboxCommand;
import com.sitionix.forge.security.server.user.ForgeUserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ResendEmailVerificationImpl implements ResendEmailVerification {

    private final AuthUserRepository authUserRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailVerificationResendPolicy emailVerificationResendPolicy;
    private final ForgeOutboxCommand<EmailVerifyPayload> outboxCommand;
    private final EmailVerifyPayloadBuilder emailVerifyPayloadBuilder;
    private final Clock clock;
    private final ForgeUserClient forgeUserClient;

    @Override
    @Transactional
    public ResendEmailVerificationResponse execute() {
        final Long userId = this.forgeUserClient.getUserId();
        final AuthUser user = this.authUserRepository.findById(userId).orElse(null);
        if (Objects.isNull(user) || !UserStatus.PENDING_EMAIL_VERIFY.equals(user.getStatus())) {
            return this.buildResponse();
        }

        if (!this.emailVerificationResendPolicy.isResendAllowed(userId)) {
            throw new EmailVerificationResendNotAllowedException(
                    "Please wait before requesting another verification email.");
        }

        this.emailVerificationTokenRepository.revokeActiveByUserId(userId);

        final EmailVerifyPayload payload = this.emailVerifyPayloadBuilder.build(this.buildContext(user));
        this.outboxCommand.send(payload);

        return this.buildResponse();
    }

    private EmailVerifyPayloadContext buildContext(final AuthUser user) {
        return new EmailVerifyPayloadContext(
                user.getId(),
                user.getSiteId(),
                user.getEmail(),
                null,
                null,
                this.clock.instant()
        );
    }

    private ResendEmailVerificationResponse buildResponse() {
        return ResendEmailVerificationResponse.builder()
                .message("If your account requires verification, an email will be sent shortly.")
                .build();
    }
}
