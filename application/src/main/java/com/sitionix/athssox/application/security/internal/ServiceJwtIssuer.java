package com.sitionix.athssox.application.security.internal;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sitionix.athssox.application.config.InternalAuthConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ServiceJwtIssuer {

    private static final long EXPIRY_SAFETY_SECONDS = 5L;

    private final InternalAuthConfig internalAuthConfig;
    private final Clock clock;
    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

    public String issueToken(final String audience, final List<String> scopes) {
        if (this.internalAuthConfig.getMode() != InternalAuthMode.DEV_JWT) {
            throw new IllegalStateException("Internal auth is not in dev-jwt mode");
        }
        final String serviceName = this.internalAuthConfig.getServiceName();
        if (!StringUtils.hasText(serviceName)) {
            throw new IllegalStateException("security.internal-auth.service-name must be configured");
        }
        if (!StringUtils.hasText(audience)) {
            throw new IllegalArgumentException("Audience must be provided");
        }
        final String cacheKey = this.buildCacheKey(audience, scopes);
        final Instant now = Instant.now(this.clock);
        final CachedToken cached = this.cache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(now.plusSeconds(EXPIRY_SAFETY_SECONDS))) {
            return cached.token();
        }
        final Instant expiresAt = now.plusSeconds(this.internalAuthConfig.getDev().getTtlSeconds());
        final Algorithm algorithm = Algorithm.HMAC256(this.internalAuthConfig.getDev().getJwtSecret());
        final String normalizedScopes = this.normalizeScopes(scopes);
        final com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
                .withIssuer(this.internalAuthConfig.getDev().getIssuer())
                .withSubject(serviceName)
                .withAudience(audience)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt));
        if (normalizedScopes != null) {
            builder.withClaim("scope", normalizedScopes);
        }
        final String token = builder.sign(algorithm);
        this.cache.put(cacheKey, new CachedToken(token, expiresAt));
        return token;
    }

    private String normalizeScopes(final List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return null;
        }
        return scopes.stream()
                .filter(StringUtils::hasText)
                .sorted(Comparator.naturalOrder())
                .reduce((left, right) -> left + " " + right)
                .orElse(null);
    }

    private String buildCacheKey(final String audience, final List<String> scopes) {
        final String scopeKey = this.normalizeScopes(scopes);
        return audience + "|" + (scopeKey == null ? "" : scopeKey);
    }

    private record CachedToken(String token, Instant expiresAt) {
    }
}
