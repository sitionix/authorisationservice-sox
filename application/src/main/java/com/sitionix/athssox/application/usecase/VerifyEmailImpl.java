package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.model.emailverify.EmailVerification;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.domain.repository.AuthUserRepository;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.service.TokenHasher;
import com.sitionix.athssox.domain.usecase.VerifyEmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyEmailImpl implements VerifyEmail {

    private final TokenHasher tokenHasher;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final AuthUserRepository authUserRepository;
    private final Clock clock;

    @Override
    @Transactional
    public boolean execute(final EmailVerification emailVerification) {
        final String hashedToken = this.tokenHasher.hash(emailVerification.getToken());
        final Optional<EmailVerificationTokenRecord> tokenRecordOptional = this.verificationTokenRepository.findByHashedToken(hashedToken);
        if (tokenRecordOptional.isEmpty()) {
            return false;
        }

        final EmailVerificationTokenRecord tokenRecord = tokenRecordOptional.get();
        final Instant now = this.clock.instant();
        if (isExpired(tokenRecord, now) || isTokenInvalid(tokenRecord) || isSiteMismatch(tokenRecord.getSiteId(), emailVerification.getSiteId())) {
            log.info("Token is invalid due to expiration, status, or site mismatch.");
            return false;
        }

        final Optional<AuthUser> user = this.authUserRepository.findById(tokenRecord.getUserId());
        if (user.isEmpty()) {
            log.info("User not found for token record: {}", tokenRecord);
            return false;
        }

        final AuthUser authUser = user.get();
        final boolean verified = authUser.getStatus() == UserStatus.PENDING_EMAIL_VERIFY;
        if (verified) {
            authUser.setStatus(UserStatus.ACTIVE);
            this.authUserRepository.save(authUser);
            this.markTokenUsed(tokenRecord, now);
        }

        return verified;
    }

    private boolean isExpired(final EmailVerificationTokenRecord tokenRecord, final Instant now) {
        return tokenRecord.getExpiresAt() == null || !tokenRecord.getExpiresAt().isAfter(now);
    }

    private boolean isTokenInvalid(final EmailVerificationTokenRecord tokenRecord) {
        return tokenRecord.getStatus() != EmailVerificationTokenStatus.ACTIVE;
    }

    private boolean isSiteMismatch(final UUID tokenSiteId, final UUID requestSiteId) {
        if (tokenSiteId == null) {
            return false;
        }
        return !tokenSiteId.equals(requestSiteId);
    }

    private void markTokenUsed(final EmailVerificationTokenRecord tokenRecord, final Instant usedAt) {
        tokenRecord.setStatus(EmailVerificationTokenStatus.USED);
        tokenRecord.setUsedAt(usedAt);
        this.verificationTokenRepository.save(tokenRecord);
    }
}
