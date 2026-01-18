package com.sitionix.athssox.api.ratelimit;

import com.sitionix.athssox.domain.service.RateLimitResult;
import com.sitionix.athssox.domain.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.apache.logging.log4j.ThreadContext;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Log4j2
@Component
@RequiredArgsConstructor
public class RateLimitGuard {

    private static final String MESSAGE_TEMPLATE = "Too many requests. Please retry after %d seconds.";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String MASKED_VALUE = "-";
    private static final String LOGIN_ENDPOINT = "login";
    private static final String REFRESH_ENDPOINT = "refresh";
    private static final String REGISTER_ENDPOINT = "register";
    private static final String RESEND_ENDPOINT = "resend";

    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;
    private final EmailNormalizer emailNormalizer;

    public void checkLogin(final String ip, final String email, final String sessionSourceId) {
        if (!this.rateLimitProperties.isEnabled()) {
            return;
        }

        final RateLimitProperties.EndpointLimits limits = this.rateLimitProperties.getLogin();
        final String normalizedEmail = this.emailNormalizer.normalize(email);
        final String emailHash = this.hashValue(normalizedEmail);
        final String maskedIp = this.maskIp(ip);

        this.enforceRule(LOGIN_ENDPOINT, "login:ip", ip, limits.getIp(), emailHash, maskedIp);
        this.enforceRule(LOGIN_ENDPOINT, "login:email", normalizedEmail, limits.getEmail(), emailHash, maskedIp);
        this.enforceRule(LOGIN_ENDPOINT, "login:ip-session", this.composeKey(ip, sessionSourceId),
                limits.getIpSession(), emailHash, maskedIp);
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
        final String normalizedEmail = this.emailNormalizer.normalize(email);
        final String emailHash = this.hashValue(normalizedEmail);
        final String maskedIp = this.maskIp(ip);

        this.enforceRule(REGISTER_ENDPOINT, "register:ip", ip, limits.getIp(), emailHash, maskedIp);
        this.enforceRule(REGISTER_ENDPOINT, "register:email", normalizedEmail, limits.getEmail(), emailHash, maskedIp);
        this.enforceRule(REGISTER_ENDPOINT, "register:ip-email", this.composeKey(ip, normalizedEmail),
                limits.getIpEmail(), emailHash, maskedIp);
    }

    public void checkResend(final String ip, final String email) {
        if (!this.rateLimitProperties.isEnabled()) {
            return;
        }

        final RateLimitProperties.EndpointLimits limits = this.rateLimitProperties.getResend();
        final String normalizedEmail = this.emailNormalizer.normalize(email);
        final String emailHash = this.hashValue(normalizedEmail);
        final String maskedIp = this.maskIp(ip);

        this.enforceRule(RESEND_ENDPOINT, "resend:ip", ip, limits.getIp(), emailHash, maskedIp);
        this.enforceRule(RESEND_ENDPOINT, "resend:email", normalizedEmail, limits.getEmail(), emailHash, maskedIp);
        this.enforceRule(RESEND_ENDPOINT, "resend:ip-email", this.composeKey(ip, normalizedEmail),
                limits.getIpEmail(), emailHash, maskedIp);
    }

    public void checkRefresh(final String ip, final String sessionSourceId) {
        if (!this.rateLimitProperties.isEnabled()) {
            return;
        }

        final RateLimitProperties.EndpointLimits limits = this.rateLimitProperties.getRefresh();
        final String maskedIp = this.maskIp(ip);
        this.enforceRule(REFRESH_ENDPOINT, "refresh:ip", ip, limits.getIp(), null, maskedIp);
        this.enforceRule(REFRESH_ENDPOINT, "refresh:session", sessionSourceId, limits.getSession(), null, maskedIp);
        this.enforceRule(REFRESH_ENDPOINT, "refresh:ip-session", this.composeKey(ip, sessionSourceId),
                limits.getIpSession(), null, maskedIp);
    }

    private void enforceRule(final String endpoint,
                             final String ruleId,
                             final String identity,
                             final RateLimitProperties.Rule rule,
                             final String emailHash,
                             final String maskedIp) {
        if (rule == null || !rule.isActive() || !StringUtils.hasText(identity)) {
            return;
        }

        final String key = ruleId + ":" + identity;
        final RateLimitResult result = this.rateLimiterService.consume(key, rule.getLimit(), rule.getWindow());
        if (result.allowed()) {
            return;
        }

        final Duration retryAfter = result.retryAfter();
        final long retryAfterSeconds = Math.max(1L, retryAfter.getSeconds());
        log.warn("Rate limit exceeded endpoint={} rule={} traceId={} ip={} emailHash={}",
                endpoint,
                ruleId,
                this.getTraceId(),
                maskedIp,
                emailHash == null ? MASKED_VALUE : emailHash);
        throw new RateLimitExceededException(String.format(MESSAGE_TEMPLATE, retryAfterSeconds), retryAfterSeconds);
    }

    private String composeKey(final String left, final String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return null;
        }
        return left + ":" + right;
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

    private String maskIp(final String ip) {
        if (!StringUtils.hasText(ip)) {
            return MASKED_VALUE;
        }
        if (ip.contains(".")) {
            final String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".0";
            }
        }
        if (ip.contains(":")) {
            final String[] parts = ip.split(":");
            if (parts.length > 0) {
                final StringBuilder builder = new StringBuilder();
                for (int i = 0; i < Math.min(4, parts.length); i++) {
                    if (i > 0) {
                        builder.append(":");
                    }
                    builder.append(parts[i]);
                }
                builder.append("::");
                return builder.toString();
            }
        }
        return ip;
    }
}
