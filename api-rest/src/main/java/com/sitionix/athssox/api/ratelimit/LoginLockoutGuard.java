package com.sitionix.athssox.api.ratelimit;

import com.sitionix.athssox.domain.service.RateLimitResult;
import com.sitionix.athssox.domain.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Log4j2
@Component
@RequiredArgsConstructor
public class LoginLockoutGuard {

    private static final String MESSAGE_TEMPLATE = "Too many requests. Please retry after %d seconds.";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String MASKED_VALUE = "-";
    private static final String FAILURE_KEY_PREFIX = "login:failure:";
    private static final String LOCKOUT_KEY_PREFIX = "login:lockout:";

    private final RateLimiterService rateLimiterService;
    private final LoginLockoutProperties properties;
    private final EmailNormalizer emailNormalizer;

    public void checkLockout(final String email) {
        if (!this.properties.isEnabled()) {
            return;
        }

        final String normalized = this.emailNormalizer.normalize(email);
        if (!StringUtils.hasText(normalized)) {
            return;
        }

        final Duration cooldown = this.properties.getCooldown();
        if (!this.isDurationValid(cooldown)) {
            return;
        }

        final String key = LOCKOUT_KEY_PREFIX + normalized;
        final RateLimitResult result = this.rateLimiterService.consume(key, 1, cooldown);
        if (result.allowed()) {
            this.rateLimiterService.reset(key);
            return;
        }

        final long retryAfterSeconds = Math.max(1L, result.retryAfter().getSeconds());
        log.warn("Login lockout active traceId={} emailHash={}", this.getTraceId(), this.hashValue(normalized));
        throw new RateLimitExceededException(String.format(MESSAGE_TEMPLATE, retryAfterSeconds), retryAfterSeconds);
    }

    public void recordFailure(final String email) {
        if (!this.properties.isEnabled()) {
            return;
        }

        final String normalized = this.emailNormalizer.normalize(email);
        if (!StringUtils.hasText(normalized)) {
            return;
        }

        final long threshold = this.properties.getFailureThreshold();
        if (threshold <= 0) {
            return;
        }

        final Duration failureWindow = this.properties.getFailureWindow();
        final Duration cooldown = this.properties.getCooldown();
        if (!this.isDurationValid(failureWindow) || !this.isDurationValid(cooldown)) {
            return;
        }

        final String failureKey = FAILURE_KEY_PREFIX + normalized;
        final long limit = Math.max(0L, threshold - 1);
        final RateLimitResult result = this.rateLimiterService.consume(failureKey, limit, failureWindow);
        final boolean shouldLock = threshold <= 1 || !result.allowed();
        if (!shouldLock) {
            return;
        }

        this.rateLimiterService.consume(LOCKOUT_KEY_PREFIX + normalized, 1, cooldown);
        this.rateLimiterService.reset(failureKey);

        log.warn("Login lockout activated traceId={} emailHash={}", this.getTraceId(), this.hashValue(normalized));
    }

    public void recordSuccess(final String email) {
        if (!this.properties.isEnabled()) {
            return;
        }

        final String normalized = this.emailNormalizer.normalize(email);
        if (!StringUtils.hasText(normalized)) {
            return;
        }

        this.rateLimiterService.reset(FAILURE_KEY_PREFIX + normalized);
        this.rateLimiterService.reset(LOCKOUT_KEY_PREFIX + normalized);
    }

    private boolean isDurationValid(final Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }

    private String getTraceId() {
        final String traceId = ThreadContext.get(TRACE_ID_KEY);
        return StringUtils.hasText(traceId) ? traceId : MASKED_VALUE;
    }

    private String hashValue(final String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            final StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (final byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (final NoSuchAlgorithmException ex) {
            return MASKED_VALUE;
        }
    }
}
