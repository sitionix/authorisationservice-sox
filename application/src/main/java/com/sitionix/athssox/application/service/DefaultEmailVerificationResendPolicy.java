package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.EmailVerificationResendConfig;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.service.EmailVerificationResendPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultEmailVerificationResendPolicy implements EmailVerificationResendPolicy {

    private static final long DAILY_WINDOW_SECONDS = 86400L;

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailVerificationResendConfig resendConfig;
    private final Clock clock;

    @Override
    public boolean isResendAllowed(final Long userId) {
        final Instant now = this.clock.instant();
        if (!this.isCooldownSatisfied(userId, now)) {
            return false;
        }
        return this.isDailyCapSatisfied(userId, now);
    }

    private boolean isCooldownSatisfied(final Long userId, final Instant now) {
        final long cooldownSeconds = this.resendConfig.getCooldownSeconds();
        if (cooldownSeconds <= 0) {
            return true;
        }

        final Optional<Instant> lastCreatedAt = this.emailVerificationTokenRepository
                .findLatestCreatedAtByUserId(userId);

        if (lastCreatedAt.isEmpty()) {
            return true;
        }

        final Instant allowedAfter = lastCreatedAt.get().plusSeconds(cooldownSeconds);
        return !allowedAfter.isAfter(now);
    }

    private boolean isDailyCapSatisfied(final Long userId, final Instant now) {
        final long dailyCap = this.resendConfig.getDailyCap();
        if (dailyCap <= 0) {
            return true;
        }

        final Instant windowStart = now.minusSeconds(DAILY_WINDOW_SECONDS);
        final long sentCount = this.emailVerificationTokenRepository
                .countByUserIdAndCreatedAtAfter(userId, windowStart);

        return sentCount < dailyCap;
    }
}
