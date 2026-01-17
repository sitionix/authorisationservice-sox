package com.sitionix.athssox.api.ratelimit;

import com.sitionix.athssox.domain.service.RateLimitResult;
import com.sitionix.athssox.domain.service.RateLimiterService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
public class RateLimitGuard {

    private static final String MESSAGE_TEMPLATE = "Too many requests. Please retry after %d seconds.";

    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;
    private final EmailNormalizer emailNormalizer;

    public RateLimitGuard(final RateLimiterService rateLimiterService,
                          final RateLimitProperties rateLimitProperties,
                          final EmailNormalizer emailNormalizer) {
        this.rateLimiterService = rateLimiterService;
        this.rateLimitProperties = rateLimitProperties;
        this.emailNormalizer = emailNormalizer;
    }

    public void checkLogin(final String ip, final String email) {
        if (!this.rateLimitProperties.isEnabled()) {
            return;
        }

        final RateLimitProperties.EndpointLimits limits = this.rateLimitProperties.getLogin();
        this.enforceRule("login:ip", ip, limits.getIp());
        this.enforceRule("login:email", this.emailNormalizer.normalize(email), limits.getEmail());
    }

    public void resetLoginEmail(final String email) {
        final String normalized = this.emailNormalizer.normalize(email);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        this.rateLimiterService.reset("login:email:" + normalized);
    }

    public void checkRegister(final String ip, final String email) {
        if (!this.rateLimitProperties.isEnabled()) {
            return;
        }

        final RateLimitProperties.EndpointLimits limits = this.rateLimitProperties.getRegister();
        this.enforceRule("register:ip", ip, limits.getIp());
        this.enforceRule("register:email", this.emailNormalizer.normalize(email), limits.getEmail());
    }

    public void checkResend(final String ip, final String email) {
        if (!this.rateLimitProperties.isEnabled()) {
            return;
        }

        final RateLimitProperties.EndpointLimits limits = this.rateLimitProperties.getResend();
        this.enforceRule("resend:ip", ip, limits.getIp());
        this.enforceRule("resend:email", this.emailNormalizer.normalize(email), limits.getEmail());
    }

    public void checkRefresh(final String ip, final String sessionSourceId) {
        if (!this.rateLimitProperties.isEnabled()) {
            return;
        }

        final RateLimitProperties.EndpointLimits limits = this.rateLimitProperties.getRefresh();
        this.enforceRule("refresh:ip", ip, limits.getIp());
        this.enforceRule("refresh:session", sessionSourceId, limits.getSession());
    }

    private void enforceRule(final String prefix,
                             final String identity,
                             final RateLimitProperties.Rule rule) {
        if (rule == null || !rule.isActive() || !StringUtils.hasText(identity)) {
            return;
        }

        final String key = prefix + ":" + identity;
        final RateLimitResult result = this.rateLimiterService.consume(key, rule.getLimit(), rule.getWindow());
        if (result.allowed()) {
            return;
        }

        final Duration retryAfter = result.retryAfter();
        final long retryAfterSeconds = Math.max(1L, retryAfter.getSeconds());
        throw new RateLimitExceededException(String.format(MESSAGE_TEMPLATE, retryAfterSeconds), retryAfterSeconds);
    }
}
