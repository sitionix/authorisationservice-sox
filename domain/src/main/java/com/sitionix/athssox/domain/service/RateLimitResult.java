package com.sitionix.athssox.domain.service;

import java.time.Duration;

public record RateLimitResult(boolean allowed, Duration retryAfter) {

    public static RateLimitResult allow() {
        return new RateLimitResult(true, Duration.ZERO);
    }

    public static RateLimitResult limited(final Duration retryAfter) {
        return new RateLimitResult(false, retryAfter == null ? Duration.ZERO : retryAfter);
    }
}
