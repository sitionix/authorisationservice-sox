package com.sitionix.athssox.application.cleanup;

import com.sitionix.athssox.application.config.CleanupConfig;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import com.sitionix.forge.outbox.core.port.OutboxStorage;
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

    private static final long REFRESH_TOKEN_RETENTION_DAYS = 14;
    private static final long EMAIL_VERIFICATION_TOKEN_RETENTION_DAYS = 2;
    private static final long OUTBOX_EVENT_RETENTION_DAYS = 14;

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final OutboxStorage outboxStorage;
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
        final int outboxDeleted = this.deleteOutboxEvents(now);

        log.info("Cleanup completed refreshTokensDeleted={} emailVerificationTokensDeleted={} outboxEventsDeleted={}",
                refreshDeleted,
                emailTokensDeleted,
                outboxDeleted);
    }

    private int deleteRefreshTokens(final Instant now) {
        if (REFRESH_TOKEN_RETENTION_DAYS <= 0) {
            return 0;
        }
        final Instant cutoff = now.minus(Duration.ofDays(REFRESH_TOKEN_RETENTION_DAYS));
        return this.refreshTokenRepository.deleteInactiveBefore(cutoff);
    }

    private int deleteEmailVerificationTokens(final Instant now) {
        if (EMAIL_VERIFICATION_TOKEN_RETENTION_DAYS <= 0) {
            return 0;
        }
        final Instant cutoff = now.minus(Duration.ofDays(EMAIL_VERIFICATION_TOKEN_RETENTION_DAYS));
        return this.emailVerificationTokenRepository.deleteExpiredBefore(cutoff);
    }

    private int deleteOutboxEvents(final Instant now) {
        if (OUTBOX_EVENT_RETENTION_DAYS <= 0) {
            return 0;
        }
        final Instant cutoff = now.minus(Duration.ofDays(OUTBOX_EVENT_RETENTION_DAYS));
        return this.outboxStorage.deleteSentBefore(cutoff);
    }
}
