package com.sitionix.athssox.domain.service;

import java.time.Duration;

public interface RateLimiterService {

    RateLimitResult consume(final String key, final long limit, final Duration window);

    void reset(final String key);
}
