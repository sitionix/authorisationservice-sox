package com.sitionix.athssox.it;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.Date;

abstract class InternalAuthITSupport {

    @Value("${security.internal-auth.dev.jwt-secret}")
    private String internalJwtSecret;

    @Value("${security.internal-auth.dev.issuer}")
    private String internalJwtIssuer;

    @Value("${security.internal-auth.dev.accepted-audiences[0]}")
    private String internalJwtAudience;

    @Value("${security.internal-auth.dev.ttl-seconds}")
    private long internalJwtTtlSeconds;

    protected String serviceToken;

    @BeforeEach
    void setUpInternalAuthToken() {
        final Instant now = Instant.now();
        this.serviceToken = JWT.create()
                .withIssuer(this.internalJwtIssuer)
                .withSubject("bffservice-sox")
                .withAudience(this.internalJwtAudience)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(this.internalJwtTtlSeconds)))
                .sign(Algorithm.HMAC256(this.internalJwtSecret));
    }

    protected String buildServiceToken(final String serviceName) {
        return this.buildServiceToken(serviceName, null);
    }

    protected String buildServiceToken(final String serviceName, final String scope) {
        final Instant now = Instant.now();
        final com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
                .withIssuer(this.internalJwtIssuer)
                .withSubject(serviceName)
                .withAudience(this.internalJwtAudience)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(this.internalJwtTtlSeconds)));
        if (scope != null) {
            builder.withClaim("scope", scope);
        }
        return builder.sign(Algorithm.HMAC256(this.internalJwtSecret));
    }
}
