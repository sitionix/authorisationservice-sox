package com.sitionix.athssox.ratelimit;

import com.github.benmanes.caffeine.cache.Expiry;
import com.sitionix.athssox.domain.service.RateLimitResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InMemoryRateLimiterTest {

    private InMemoryRateLimiter inMemoryRateLimiter;

    @Mock
    private Clock clock;

    private AtomicReference<Instant> currentInstant;

    @BeforeEach
    void setUp() {
        this.currentInstant = this.getInstantReference(this.getInstant("2025-01-01T00:00:00Z"));
        this.inMemoryRateLimiter = new InMemoryRateLimiter(this.clock);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.clock);
    }

    @Test
    void givenLimitLessThanOrEqualZero_whenConsume_thenAllow() {
        //given
        final String key = "rate-limit-key";
        final Duration window = this.getDurationSeconds(5L);
        final RateLimitResult expected = this.getAllowedResult();

        //when
        final RateLimitResult actual = this.inMemoryRateLimiter.consume(key, 0L, window);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenNullWindow_whenConsume_thenAllow() {
        //given
        final String key = "rate-limit-key";
        final Duration window = null;
        final RateLimitResult expected = this.getAllowedResult();

        //when
        final RateLimitResult actual = this.inMemoryRateLimiter.consume(key, 1L, window);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenZeroWindow_whenConsume_thenAllow() {
        //given
        final String key = "rate-limit-key";
        final Duration window = this.getDurationSeconds(0L);
        final RateLimitResult expected = this.getAllowedResult();

        //when
        final RateLimitResult actual = this.inMemoryRateLimiter.consume(key, 1L, window);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenNegativeWindow_whenConsume_thenAllow() {
        //given
        final String key = "rate-limit-key";
        final Duration window = this.getDurationSeconds(-1L);
        final RateLimitResult expected = this.getAllowedResult();

        //when
        final RateLimitResult actual = this.inMemoryRateLimiter.consume(key, 1L, window);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenLimitExceeded_whenConsume_thenReturnLimitedResult() {
        //given
        final String key = "rate-limit-key";
        final long limit = 2L;
        final Duration window = this.getDurationSeconds(10L);
        final RateLimitResult allowed = this.getAllowedResult();
        final RateLimitResult expected = this.getLimitedResult(window);
        this.stubClockInstant();

        //when
        final RateLimitResult first = this.inMemoryRateLimiter.consume(key, limit, window);
        final RateLimitResult second = this.inMemoryRateLimiter.consume(key, limit, window);
        final RateLimitResult actual = this.inMemoryRateLimiter.consume(key, limit, window);

        //then
        assertThat(first).isEqualTo(allowed);
        assertThat(second).isEqualTo(allowed);
        assertThat(actual).isEqualTo(expected);
        verify(this.clock, atLeastOnce())
                .instant();
    }

    @Test
    void givenExpiredWindow_whenConsume_thenResetCountAndAllow() {
        //given
        final String key = "rate-limit-key";
        final long limit = 1L;
        final Duration window = this.getDurationSeconds(5L);
        final RateLimitResult allowed = this.getAllowedResult();
        final Instant initialInstant = this.getCurrentInstant();
        final Instant expiredInstant = this.getInstantPlusSeconds(initialInstant, 6L);
        this.stubClockInstant();

        //when
        final RateLimitResult first = this.inMemoryRateLimiter.consume(key, limit, window);
        this.setCurrentInstant(expiredInstant);
        final RateLimitResult actual = this.inMemoryRateLimiter.consume(key, limit, window);

        //then
        assertThat(first).isEqualTo(allowed);
        assertThat(actual).isEqualTo(allowed);
        verify(this.clock, atLeastOnce())
                .instant();
    }

    @Test
    void givenResetCalled_whenConsumeAfterReset_thenAllow() {
        //given
        final String key = "rate-limit-key";
        final long limit = 1L;
        final Duration window = this.getDurationSeconds(10L);
        final RateLimitResult allowed = this.getAllowedResult();
        final RateLimitResult limited = this.getLimitedResult(window);
        this.stubClockInstant();

        //when
        final RateLimitResult first = this.inMemoryRateLimiter.consume(key, limit, window);
        final RateLimitResult second = this.inMemoryRateLimiter.consume(key, limit, window);
        this.inMemoryRateLimiter.reset(key);
        final RateLimitResult actual = this.inMemoryRateLimiter.consume(key, limit, window);

        //then
        assertThat(first).isEqualTo(allowed);
        assertThat(second).isEqualTo(limited);
        assertThat(actual).isEqualTo(allowed);
        verify(this.clock, atLeastOnce())
                .instant();
    }

    @Test
    void givenNullKey_whenReset_thenKeepExistingBucket() {
        //given
        final String key = "rate-limit-key";
        final long limit = 1L;
        final Duration window = this.getDurationSeconds(10L);
        final RateLimitResult allowed = this.getAllowedResult();
        final RateLimitResult limited = this.getLimitedResult(window);
        this.stubClockInstant();

        //when
        final RateLimitResult first = this.inMemoryRateLimiter.consume(key, limit, window);
        this.inMemoryRateLimiter.reset(null);
        final RateLimitResult actual = this.inMemoryRateLimiter.consume(key, limit, window);

        //then
        assertThat(first).isEqualTo(allowed);
        assertThat(actual).isEqualTo(limited);
        verify(this.clock, atLeastOnce())
                .instant();
    }

    @Test
    void givenExpiryWithFutureInstant_whenExpireAfterCreate_thenReturnRemainingNanos() {
        //given
        final Expiry expiry = this.getRateLimitExpiry(this.clock);
        final Instant now = this.getCurrentInstant();
        final Instant expiresAt = this.getInstantPlusSeconds(now, 5L);
        final Object bucket = this.getRateLimitBucket(1L, expiresAt);
        final Duration expectedDuration = this.getDurationBetween(now, expiresAt);
        final long expected = expectedDuration.toNanos();
        this.stubClockInstant();

        //when
        final long actual = this.expireAfterCreate(expiry, bucket);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.clock, atLeastOnce())
                .instant();
    }

    @Test
    void givenExpiryWithPastInstant_whenExpireAfterCreate_thenReturnZero() {
        //given
        final Expiry expiry = this.getRateLimitExpiry(this.clock);
        final Instant now = this.getCurrentInstant();
        final Instant expiresAt = this.getInstantPlusSeconds(now, -5L);
        final Object bucket = this.getRateLimitBucket(1L, expiresAt);
        final long expected = this.getDurationSeconds(0L).toNanos();
        this.stubClockInstant();

        //when
        final long actual = this.expireAfterCreate(expiry, bucket);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.clock, atLeastOnce())
                .instant();
    }

    @Test
    void givenExpiryWithZeroDuration_whenExpireAfterCreate_thenReturnZero() {
        //given
        final Expiry expiry = this.getRateLimitExpiry(this.clock);
        final Instant now = this.getCurrentInstant();
        final Instant expiresAt = this.getInstantPlusSeconds(now, 0L);
        final Object bucket = this.getRateLimitBucket(1L, expiresAt);
        final long expected = this.getDurationSeconds(0L).toNanos();
        this.stubClockInstant();

        //when
        final long actual = this.expireAfterCreate(expiry, bucket);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.clock, atLeastOnce())
                .instant();
    }

    @Test
    void givenExpiryWithFutureInstant_whenExpireAfterUpdate_thenReturnRemainingNanos() {
        //given
        final Expiry expiry = this.getRateLimitExpiry(this.clock);
        final Instant now = this.getCurrentInstant();
        final Instant expiresAt = this.getInstantPlusSeconds(now, 8L);
        final Object bucket = this.getRateLimitBucket(1L, expiresAt);
        final Duration expectedDuration = this.getDurationBetween(now, expiresAt);
        final long expected = expectedDuration.toNanos();
        final long currentDuration = 123L;
        this.stubClockInstant();

        //when
        final long actual = this.expireAfterUpdate(expiry, bucket, currentDuration);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.clock, atLeastOnce())
                .instant();
    }

    @Test
    void givenExpiryWithCurrentDuration_whenExpireAfterRead_thenReturnCurrentDuration() {
        //given
        final Expiry expiry = this.getRateLimitExpiry(this.clock);
        final Instant now = this.getCurrentInstant();
        final Instant expiresAt = this.getInstantPlusSeconds(now, 4L);
        final Object bucket = this.getRateLimitBucket(1L, expiresAt);
        final long currentDuration = 456L;

        //when
        final long actual = this.expireAfterRead(expiry, bucket, currentDuration);

        //then
        assertThat(actual).isEqualTo(currentDuration);
    }

    private Duration getDurationSeconds(final long seconds) {
        return Duration.ofSeconds(seconds);
    }

    private Duration getDurationBetween(final Instant start, final Instant end) {
        return Duration.between(start, end);
    }

    private RateLimitResult getAllowedResult() {
        return RateLimitResult.allow();
    }

    private RateLimitResult getLimitedResult(final Duration retryAfter) {
        return RateLimitResult.limited(retryAfter);
    }

    private Instant getInstant(final String value) {
        return Instant.parse(value);
    }

    private AtomicReference<Instant> getInstantReference(final Instant instant) {
        return new AtomicReference<>(instant);
    }

    private Instant getInstantPlusSeconds(final Instant base, final long seconds) {
        return base.plusSeconds(seconds);
    }

    private Instant getCurrentInstant() {
        return this.currentInstant.get();
    }

    private void setCurrentInstant(final Instant instant) {
        this.currentInstant.set(instant);
    }

    private void stubClockInstant() {
        when(this.clock.instant())
                .thenAnswer(invocation -> this.currentInstant.get());
    }

    @SuppressWarnings("rawtypes")
    private Expiry getRateLimitExpiry(final Clock expiryClock) {
        try {
            final Class<?> expiryClass = Class.forName("com.sitionix.athssox.ratelimit.InMemoryRateLimiter$RateLimitExpiry");
            final Constructor<?> constructor = expiryClass.getDeclaredConstructor(Clock.class);
            constructor.setAccessible(true);
            return (Expiry) constructor.newInstance(expiryClock);
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create RateLimitExpiry.", ex);
        }
    }

    private Object getRateLimitBucket(final long count, final Instant expiresAt) {
        try {
            final Class<?> bucketClass = Class.forName("com.sitionix.athssox.ratelimit.InMemoryRateLimiter$RateLimitBucket");
            final Constructor<?> constructor = bucketClass.getDeclaredConstructor(long.class, Instant.class);
            constructor.setAccessible(true);
            return constructor.newInstance(count, expiresAt);
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create RateLimitBucket.", ex);
        }
    }

    @SuppressWarnings("rawtypes")
    private long expireAfterCreate(final Expiry expiry, final Object bucket) {
        return expiry.expireAfterCreate("expiry-key", bucket, 0L);
    }

    @SuppressWarnings("rawtypes")
    private long expireAfterUpdate(final Expiry expiry, final Object bucket, final long currentDuration) {
        return expiry.expireAfterUpdate("expiry-key", bucket, 0L, currentDuration);
    }

    @SuppressWarnings("rawtypes")
    private long expireAfterRead(final Expiry expiry, final Object bucket, final long currentDuration) {
        return expiry.expireAfterRead("expiry-key", bucket, 0L, currentDuration);
    }
}
