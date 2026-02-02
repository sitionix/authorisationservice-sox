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

        if (this.internalAuthConfig.getProtectedEndpoints() == null
                || this.internalAuthConfig.getProtectedEndpoints().isEmpty()) {
            throw new IllegalStateException("security.internal-auth.protected-endpoints must be configured.");
        }

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
        final List<String> audience = devConfig.getAudience();
        if (audience == null || audience.isEmpty()) {
            throw new IllegalStateException("security.internal-auth.dev.audience must be configured for dev-jwt.");
        }
        if (devConfig.getTtlSeconds() <= 0) {
            throw new IllegalStateException("security.internal-auth.dev.ttl-seconds must be positive for dev-jwt.");
        }
        if (!StringUtils.hasText(this.internalAuthConfig.getServiceName())) {
            throw new IllegalStateException("security.internal-auth.service-name must be configured for dev-jwt.");
        }
    }
}
