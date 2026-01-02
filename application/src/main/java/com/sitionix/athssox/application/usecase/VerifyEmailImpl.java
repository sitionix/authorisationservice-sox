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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

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
        final Optional<EmailVerificationTokenRecord> tokenRecord = this.verificationTokenRepository.findByHashedToken(hashedToken);
        if (tokenRecord.isEmpty()) {
            return false;
        }

        final EmailVerificationTokenRecord record = tokenRecord.get();
        final Instant now = this.clock.instant();
        if (isExpired(record, now) || isTokenInvalid(record) || isSiteMismatch(record.getSiteId(), emailVerification.getSiteId())) {
            return false;
        }

        final Optional<AuthUser> user = this.authUserRepository.findById(record.getUserId());
        if (user.isEmpty()) {
            return false;
        }

        final AuthUser authUser = user.get();
        final boolean verified = authUser.getStatus() == UserStatus.PENDING_EMAIL_VERIFY;
        if (verified) {
            authUser.setStatus(UserStatus.ACTIVE);
            this.authUserRepository.save(authUser);
        }

        this.markTokenUsed(record, now);

        return verified;
    }

    private boolean isExpired(final EmailVerificationTokenRecord record, final Instant now) {
        return record.getExpiresAt() == null || !record.getExpiresAt().isAfter(now);
    }

    private boolean isTokenInvalid(final EmailVerificationTokenRecord record) {
        return record.getStatus() != EmailVerificationTokenStatus.ACTIVE;
    }

    private boolean isSiteMismatch(final UUID tokenSiteId, final UUID requestSiteId) {
        if (tokenSiteId == null) {
            return requestSiteId != null;
        }
        return !tokenSiteId.equals(requestSiteId);
    }

    private void markTokenUsed(final EmailVerificationTokenRecord record, final Instant usedAt) {
        record.setStatus(EmailVerificationTokenStatus.USED);
        record.setUsedAt(usedAt);
        this.verificationTokenRepository.save(record);
    }
}
