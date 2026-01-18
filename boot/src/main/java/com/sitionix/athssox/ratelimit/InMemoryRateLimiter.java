package com.sitionix.athssox.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.sitionix.athssox.domain.service.RateLimitResult;
import com.sitionix.athssox.domain.service.RateLimiterService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class InMemoryRateLimiter implements RateLimiterService {

    private final Clock clock;
    private final Cache<String, RateLimitBucket> cache;

    public InMemoryRateLimiter(final Clock clock) {
        this.clock = clock;
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfter(new RateLimitExpiry(this.clock))
                .build();
    }

    @Override
    public RateLimitResult consume(final String key, final long limit, final Duration window) {
        if (limit <= 0 || window == null || window.isZero() || window.isNegative()) {
            return RateLimitResult.allow();
        }

        final Instant now = this.clock.instant();
        final AtomicReference<RateLimitResult> resultRef = new AtomicReference<>(RateLimitResult.allow());

        this.cache.asMap().compute(key, (k, existing) -> {
            RateLimitBucket bucket = existing;
            if (bucket == null || bucket.expiresAt().isBefore(now)) {
                bucket = new RateLimitBucket(0, now.plus(window));
            }

            final long nextCount = bucket.count() + 1;
            final RateLimitBucket updated = new RateLimitBucket(nextCount, bucket.expiresAt());

            if (nextCount > limit) {
                final Duration retryAfter = Duration.between(now, bucket.expiresAt());
                resultRef.set(RateLimitResult.limited(retryAfter.isNegative() ? Duration.ZERO : retryAfter));
            } else {
                resultRef.set(RateLimitResult.allow());
            }

            return updated;
        });

        return resultRef.get();
    }

    @Override
    public void reset(final String key) {
        if (key == null) {
            return;
        }
        this.cache.invalidate(key);
    }

    private record RateLimitBucket(long count, Instant expiresAt) {
    }

    private static final class RateLimitExpiry implements Expiry<String, RateLimitBucket> {

        private final Clock clock;

        private RateLimitExpiry(final Clock clock) {
            this.clock = clock;
        }

        @Override
        public long expireAfterCreate(final String key, final RateLimitBucket value, final long currentTime) {
            return this.toNanos(value);
        }

        @Override
        public long expireAfterUpdate(final String key,
                                      final RateLimitBucket value,
                                      final long currentTime,
                                      final long currentDuration) {
            return this.toNanos(value);
        }

        @Override
        public long expireAfterRead(final String key,
                                    final RateLimitBucket value,
                                    final long currentTime,
                                    final long currentDuration) {
            return currentDuration;
        }

        private long toNanos(final RateLimitBucket value) {
            final Instant now = this.clock.instant();
            final Duration remaining = Duration.between(now, value.expiresAt());
            if (remaining.isNegative() || remaining.isZero()) {
                return 0L;
            }
            return remaining.toNanos();
        }
    }
}
