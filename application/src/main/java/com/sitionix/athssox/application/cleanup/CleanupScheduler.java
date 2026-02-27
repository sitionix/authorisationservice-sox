package com.sitionix.athssox.application.cleanup;

import com.sitionix.athssox.application.config.CleanupConfig;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final CleanupConfig cleanupConfig;
    private final Clock clock;

    @Scheduled(cron = "${auth.cleanup.cron:0 0 0 * * *}", zone = "${auth.cleanup.zone:Europe/Kiev}")
    @Transactional
    public void runCleanup() {
        if (!this.cleanupConfig.isEnabled()) {
            return;
        }

        final Instant now = this.clock.instant();

        final int refreshDeleted = this.deleteRefreshTokens(now);
        final int emailTokensDeleted = this.deleteEmailVerificationTokens(now);

        log.info("Cleanup completed refreshTokensDeleted={} emailVerificationTokensDeleted={}",
                refreshDeleted,
                emailTokensDeleted);
    }

    private int deleteRefreshTokens(final Instant now) {
        final Duration retention = this.cleanupConfig.getRefreshTokenRetention();
        if (retention == null || retention.isZero() || retention.isNegative()) {
            return 0;
        }
        final Instant cutoff = now.minus(retention);
        return this.refreshTokenRepository.deleteInactiveBefore(cutoff);
    }

    private int deleteEmailVerificationTokens(final Instant now) {
        final Duration retention = this.cleanupConfig.getEmailVerificationTokenRetention();
        if (retention == null || retention.isZero() || retention.isNegative()) {
            return 0;
        }
        final Instant cutoff = now.minus(retention);
        return this.emailVerificationTokenRepository.deleteExpiredBefore(cutoff);
    }
}
