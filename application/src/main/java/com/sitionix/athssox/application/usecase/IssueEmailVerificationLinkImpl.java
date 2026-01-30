package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.domain.exception.EmailVerificationTokenExpiredException;
import com.sitionix.athssox.domain.exception.EmailVerificationTokenInvalidException;
import com.sitionix.athssox.domain.exception.EmailVerificationTokenNotFoundException;
import com.sitionix.athssox.domain.exception.UserAlreadyVerifiedException;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationLinkIssue;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.domain.repository.AuthUserRepository;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.service.TokenHasher;
import com.sitionix.athssox.domain.service.EmailVerificationTokenSigner;
import com.sitionix.athssox.domain.usecase.IssueEmailVerificationLink;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueEmailVerificationLinkImpl implements IssueEmailVerificationLink {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final AuthUserRepository authUserRepository;
    private final EmailVerificationTokenSigner tokenSigner;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    @Override
    @Transactional
    public EmailVerificationLinkIssue execute(final UUID tokenId, final UUID pepperId) {
        final EmailVerificationTokenRecord tokenRecord = this.emailVerificationTokenRepository.findById(tokenId)
                .orElseThrow(() -> new EmailVerificationTokenNotFoundException("Email verification token not found."));

        final Instant now = this.clock.instant();
        if (isExpired(tokenRecord, now)) {
            throw new EmailVerificationTokenExpiredException("Email verification token expired.");
        }

        if (isTokenInvalid(tokenRecord)) {
            throw new EmailVerificationTokenInvalidException("Email verification token is not active.");
        }

        final AuthUser user = this.authUserRepository.findById(tokenRecord.getUserId())
                .orElseThrow(() -> new EmailVerificationTokenNotFoundException("User not found for email verification token."));

        if (!UserStatus.PENDING_EMAIL_VERIFY.equals(user.getStatus())) {
            throw new UserAlreadyVerifiedException("User already verified.");
        }

        final String token = this.tokenSigner.buildToken(tokenRecord.getId(), pepperId);
        final String expectedTokenHash = this.tokenHasher.hash(token);

        if (!Objects.equals(expectedTokenHash, tokenRecord.getTokenHash())) {
            log.info("Email verification token hash mismatch.");
            throw new EmailVerificationTokenInvalidException("Email verification token is invalid.");
        }

        return new EmailVerificationLinkIssue(tokenRecord.getId(),
                tokenRecord.getSiteId(),
                token,
                tokenRecord.getExpiresAt());
    }

    private boolean isExpired(final EmailVerificationTokenRecord tokenRecord, final Instant now) {
        return Objects.isNull(tokenRecord.getExpiresAt()) || !tokenRecord.getExpiresAt().isAfter(now);
    }

    private boolean isTokenInvalid(final EmailVerificationTokenRecord tokenRecord) {
        return !EmailVerificationTokenStatus.ACTIVE.equals(tokenRecord.getStatus())
                || Objects.nonNull(tokenRecord.getUsedAt());
    }
}
