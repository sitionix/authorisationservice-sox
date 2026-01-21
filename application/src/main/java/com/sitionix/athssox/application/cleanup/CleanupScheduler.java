package com.sitionix.athssox.application.cleanup;

import com.sitionix.athssox.application.config.CleanupConfig;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final CleanupConfig cleanupConfig;
    private final Clock clock;

    @Scheduled(cron = "${auth.cleanup.cron:0 0 0 * * *}", zone = "${auth.cleanup.zone:Europe/Kiev}")
    @Transactional
    public void runCleanup() {
        if (!this.cleanupConfig.isEnabled()) {
            return;
        }

        final Instant now = this.clock.instant();
        final ZoneId zoneId = ZoneId.of(this.cleanupConfig.getZone());
        final LocalDateTime nowLocal = LocalDateTime.ofInstant(now, zoneId);

        final int refreshDeleted = this.deleteRefreshTokens(now);
        final int emailTokensDeleted = this.deleteEmailVerificationTokens(now);
        final int outboxDeleted = this.deleteOutboxEvents(nowLocal);

        log.info("Cleanup completed refreshTokensDeleted={} emailVerificationTokensDeleted={} outboxEventsDeleted={}",
                refreshDeleted,
                emailTokensDeleted,
                outboxDeleted);
    }

    private int deleteRefreshTokens(final Instant now) {
        final long retentionDays = this.cleanupConfig.getRefreshTokens().getRetentionDays();
        if (retentionDays <= 0) {
            return 0;
        }
        final Instant cutoff = now.minus(Duration.ofDays(retentionDays));
        return this.refreshTokenRepository.deleteInactiveBefore(cutoff);
    }

    private int deleteEmailVerificationTokens(final Instant now) {
        final long retentionDays = this.cleanupConfig.getEmailVerificationTokens().getRetentionDays();
        if (retentionDays <= 0) {
            return 0;
        }
        final Instant cutoff = now.minus(Duration.ofDays(retentionDays));
        return this.emailVerificationTokenRepository.deleteExpiredBefore(cutoff);
    }

    private int deleteOutboxEvents(final LocalDateTime now) {
        final long retentionDays = this.cleanupConfig.getOutboxEvents().getRetentionDays();
        if (retentionDays <= 0) {
            return 0;
        }
        final LocalDateTime cutoff = now.minusDays(retentionDays);
        return this.outboxEventRepository.deleteSentBefore(cutoff);
    }
}
