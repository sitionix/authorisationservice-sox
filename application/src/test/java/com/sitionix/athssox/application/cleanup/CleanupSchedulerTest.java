package com.sitionix.athssox.application.cleanup;

import com.sitionix.athssox.application.config.CleanupConfig;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
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
    private CleanupConfig cleanupConfig;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        this.cleanupScheduler = new CleanupScheduler(this.refreshTokenRepository,
                this.emailVerificationTokenRepository,
                this.cleanupConfig,
                this.clock);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.refreshTokenRepository,
                this.emailVerificationTokenRepository,
                this.cleanupConfig,
                this.clock);
    }

    @Test
    void givenCleanupDisabled_whenRunCleanup_thenSkipAllOperations() {
        //given
        when(this.cleanupConfig.isEnabled()).thenReturn(false);

        //when
        this.cleanupScheduler.runCleanup();

        //then
        verify(this.cleanupConfig).isEnabled();
    }

    @Test
    void givenCleanupEnabled_whenRunCleanup_thenDeleteTokensByConfiguredRetention() {
        //given
        final Instant now = Instant.parse("2026-02-10T10:15:30Z");
        final Duration refreshRetention = Duration.ofDays(14);
        final Duration emailRetention = Duration.ofDays(2);
        final Instant refreshTokenCutoff = now.minus(refreshRetention);
        final Instant emailTokenCutoff = now.minus(emailRetention);
        when(this.cleanupConfig.isEnabled()).thenReturn(true);
        when(this.cleanupConfig.getRefreshTokenRetention()).thenReturn(refreshRetention);
        when(this.cleanupConfig.getEmailVerificationTokenRetention()).thenReturn(emailRetention);
        when(this.clock.instant()).thenReturn(now);
        when(this.refreshTokenRepository.deleteInactiveBefore(refreshTokenCutoff)).thenReturn(1);
        when(this.emailVerificationTokenRepository.deleteExpiredBefore(emailTokenCutoff)).thenReturn(2);

        //when
        this.cleanupScheduler.runCleanup();

        //then
        verify(this.cleanupConfig).isEnabled();
        verify(this.cleanupConfig).getRefreshTokenRetention();
        verify(this.cleanupConfig).getEmailVerificationTokenRetention();
        verify(this.clock).instant();
        verify(this.refreshTokenRepository).deleteInactiveBefore(refreshTokenCutoff);
        verify(this.emailVerificationTokenRepository).deleteExpiredBefore(emailTokenCutoff);
    }
}
