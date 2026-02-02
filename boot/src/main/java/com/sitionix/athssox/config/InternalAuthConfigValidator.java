package com.sitionix.athssox.config;

import com.sitionix.athssox.application.config.InternalAuthConfig;
import com.sitionix.athssox.application.security.internal.InternalAuthMode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InternalAuthConfigValidator {

    private final InternalAuthConfig internalAuthConfig;
    private final Environment environment;

    @PostConstruct
    void validate() {
        final InternalAuthMode mode = this.internalAuthConfig.getMode();
        final boolean isProd = Arrays.stream(this.environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));

        if (isProd) {
            if (mode == null || mode != InternalAuthMode.MTLS) {
                throw new IllegalStateException("security.internal-auth.mode must be mtls in prod.");
            }
            if (StringUtils.hasText(this.internalAuthConfig.getDev().getJwtSecret())) {
                throw new IllegalStateException("security.internal-auth.dev.jwt-secret must not be set in prod.");
            }
        }

        if (mode == null) {
            throw new IllegalStateException("security.internal-auth.mode must be configured.");
        }

        this.validatePolicies();

        if (mode == InternalAuthMode.DEV_JWT) {
            this.validateDevJwt();
        }
    }

    private void validateDevJwt() {
        final InternalAuthConfig.DevJwtConfig devConfig = this.internalAuthConfig.getDev();
        if (!StringUtils.hasText(devConfig.getJwtSecret())) {
            throw new IllegalStateException("security.internal-auth.dev.jwt-secret must be configured for dev-jwt.");
        }
        if (!StringUtils.hasText(devConfig.getIssuer())) {
            throw new IllegalStateException("security.internal-auth.dev.issuer must be configured for dev-jwt.");
        }
        final List<String> audience = devConfig.getAcceptedAudiences();
        if (audience == null || audience.isEmpty() || audience.stream().noneMatch(StringUtils::hasText)) {
            throw new IllegalStateException("security.internal-auth.dev.accepted-audiences must be configured for dev-jwt.");
        }
        if (devConfig.getTtlSeconds() <= 0) {
            throw new IllegalStateException("security.internal-auth.dev.ttl-seconds must be positive for dev-jwt.");
        }
        if (!StringUtils.hasText(this.internalAuthConfig.getServiceName())) {
            throw new IllegalStateException("security.internal-auth.service-name must be configured for dev-jwt.");
        }
    }

    private void validatePolicies() {
        if (this.internalAuthConfig.getPolicies() == null || this.internalAuthConfig.getPolicies().isEmpty()) {
            return;
        }
        this.internalAuthConfig.getPolicies().forEach((serviceName, policy) -> {
            if (policy == null || policy.getAllow() == null || policy.getAllow().isEmpty()) {
                return;
            }
            final boolean allowAll = policy.getAllow().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .anyMatch("*"::equals);
            if (allowAll) {
                return;
            }
            for (final String entry : policy.getAllow()) {
                if (!this.isValidPolicyEntry(entry)) {
                    throw new IllegalStateException("Invalid internal-auth policy entry for " + serviceName + ": " + entry);
                }
            }
        });
    }

    private boolean isValidPolicyEntry(final String entry) {
        if (!StringUtils.hasText(entry)) {
            return false;
        }
        final String trimmed = entry.trim();
        if ("*".equals(trimmed)) {
            return true;
        }
        if (trimmed.startsWith("scope:")) {
            return StringUtils.hasText(trimmed.substring("scope:".length()));
        }
        final String normalized = trimmed.startsWith("endpoint:")
                ? trimmed.substring("endpoint:".length()).trim()
                : trimmed;
        final String method = this.extractMethod(normalized);
        if (method == null) {
            return false;
        }
        final String path = normalized.substring(method.length() + 1).trim();
        return StringUtils.hasText(path) && path.startsWith("/");
    }

    private String extractMethod(final String entry) {
        final String upper = entry.trim().toUpperCase();
        for (final String method : new String[]{"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"}) {
            if (upper.startsWith(method + " ") || upper.startsWith(method + ":")) {
                return method;
            }
        }
        return null;
    }
}
