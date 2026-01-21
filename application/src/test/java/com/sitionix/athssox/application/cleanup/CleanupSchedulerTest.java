package com.sitionix.athssox.application.cleanup;

import com.sitionix.athssox.application.config.CleanupConfig;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanupSchedulerTest {

    private CleanupScheduler cleanupScheduler;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private CleanupConfig cleanupConfig;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        this.cleanupScheduler = new CleanupScheduler(this.refreshTokenRepository,
                this.emailVerificationTokenRepository,
                this.outboxEventRepository,
                this.cleanupConfig,
                this.clock);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.refreshTokenRepository,
                this.emailVerificationTokenRepository,
                this.outboxEventRepository,
                this.cleanupConfig,
                this.clock);
    }

    @Test
    void given_cleanup_disabled_when_run_cleanup_then_skip_all_operations() {
        //given
        when(this.cleanupConfig.isEnabled())
                .thenReturn(false);

        //when
        this.cleanupScheduler.runCleanup();

        //then
        verify(this.cleanupConfig)
                .isEnabled();
    }

    @Test
    void given_cleanup_enabled_when_run_cleanup_then_delete_with_expected_cutoffs() {
        //given
        final Instant now = this.getNow();
        final ZoneId zoneId = this.getZoneId();
        final String zone = zoneId.getId();
        final Instant refreshTokenCutoff = this.getRefreshTokenCutoff(now);
        final Instant emailTokenCutoff = this.getEmailTokenCutoff(now);
        final LocalDateTime outboxCutoff = this.getOutboxCutoff(now, zoneId);

        when(this.cleanupConfig.isEnabled())
                .thenReturn(true);
        when(this.cleanupConfig.getZone())
                .thenReturn(zone);
        when(this.clock.instant())
                .thenReturn(now);
        when(this.refreshTokenRepository.deleteInactiveBefore(refreshTokenCutoff))
                .thenReturn(1);
        when(this.emailVerificationTokenRepository.deleteExpiredBefore(emailTokenCutoff))
                .thenReturn(2);
        when(this.outboxEventRepository.deleteSentBefore(outboxCutoff))
                .thenReturn(3);

        //when
        this.cleanupScheduler.runCleanup();

        //then
        verify(this.cleanupConfig)
                .isEnabled();
        verify(this.cleanupConfig)
                .getZone();
        verify(this.clock)
                .instant();
        verify(this.refreshTokenRepository)
                .deleteInactiveBefore(refreshTokenCutoff);
        verify(this.emailVerificationTokenRepository)
                .deleteExpiredBefore(emailTokenCutoff);
        verify(this.outboxEventRepository)
                .deleteSentBefore(outboxCutoff);
    }

    private Instant getNow() {
        return Instant.parse("2024-01-10T10:15:30Z");
    }

    private ZoneId getZoneId() {
        return ZoneId.of("Europe/Kiev");
    }

    private Instant getRefreshTokenCutoff(final Instant now) {
        return now.minus(Duration.ofDays(14));
    }

    private Instant getEmailTokenCutoff(final Instant now) {
        return now.minus(Duration.ofDays(2));
    }

    private LocalDateTime getOutboxCutoff(final Instant now, final ZoneId zoneId) {
        return LocalDateTime.ofInstant(now, zoneId).minusDays(14);
    }
}
