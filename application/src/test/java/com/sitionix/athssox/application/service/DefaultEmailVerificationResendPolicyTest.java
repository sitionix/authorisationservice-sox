package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.EmailVerificationResendConfig;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.service.EmailVerificationResendPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultEmailVerificationResendPolicyTest {

    private EmailVerificationResendPolicy resendPolicy;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @BeforeEach
    void setUp() {
        final EmailVerificationResendConfig config = this.getResendConfig(60L, 5L);
        final Clock clock = this.getFixedClock(this.getNow());
        this.resendPolicy = new DefaultEmailVerificationResendPolicy(this.emailVerificationTokenRepository,
                config,
                clock);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.emailVerificationTokenRepository);
    }

    @Test
    void given_no_previous_tokens_when_is_resend_allowed_then_return_true() {
        //given
        final Long userId = 10L;
        final Instant windowStart = this.getWindowStart();

        when(this.emailVerificationTokenRepository.findLatestCreatedAtByUserId(userId))
                .thenReturn(Optional.empty());
        when(this.emailVerificationTokenRepository.countByUserIdAndCreatedAtAfter(userId, windowStart))
                .thenReturn(0L);

        //when
        final boolean actual = this.resendPolicy.isResendAllowed(userId);

        //then
        assertThat(actual).isTrue();
        verify(this.emailVerificationTokenRepository)
                .findLatestCreatedAtByUserId(userId);
        verify(this.emailVerificationTokenRepository)
                .countByUserIdAndCreatedAtAfter(userId, windowStart);
    }

    @Test
    void given_cooldown_not_elapsed_when_is_resend_allowed_then_return_false() {
        //given
        final Long userId = 11L;
        final Instant lastCreatedAt = this.getInstant("2024-01-01T09:59:40Z");

        when(this.emailVerificationTokenRepository.findLatestCreatedAtByUserId(userId))
                .thenReturn(Optional.of(lastCreatedAt));

        //when
        final boolean actual = this.resendPolicy.isResendAllowed(userId);

        //then
        assertThat(actual).isFalse();
        verify(this.emailVerificationTokenRepository)
                .findLatestCreatedAtByUserId(userId);
    }

    @Test
    void given_daily_cap_reached_when_is_resend_allowed_then_return_false() {
        //given
        final Long userId = 12L;
        final Instant lastCreatedAt = this.getInstant("2024-01-01T09:58:00Z");
        final Instant windowStart = this.getWindowStart();

        when(this.emailVerificationTokenRepository.findLatestCreatedAtByUserId(userId))
                .thenReturn(Optional.of(lastCreatedAt));
        when(this.emailVerificationTokenRepository.countByUserIdAndCreatedAtAfter(userId, windowStart))
                .thenReturn(5L);

        //when
        final boolean actual = this.resendPolicy.isResendAllowed(userId);

        //then
        assertThat(actual).isFalse();
        verify(this.emailVerificationTokenRepository)
                .findLatestCreatedAtByUserId(userId);
        verify(this.emailVerificationTokenRepository)
                .countByUserIdAndCreatedAtAfter(userId, windowStart);
    }

    private EmailVerificationResendConfig getResendConfig(final long cooldownSeconds,
                                                          final long dailyCap) {
        final EmailVerificationResendConfig config = new EmailVerificationResendConfig();
        config.setCooldownSeconds(cooldownSeconds);
        config.setDailyCap(dailyCap);
        return config;
    }

    private Clock getFixedClock(final Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private Instant getNow() {
        return this.getInstant("2024-01-01T10:00:00Z");
    }

    private Instant getWindowStart() {
        return this.getInstant("2023-12-31T10:00:00Z");
    }

    private Instant getInstant(final String value) {
        return Instant.parse(value);
    }
}
