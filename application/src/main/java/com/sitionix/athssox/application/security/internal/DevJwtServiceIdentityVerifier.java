package com.sitionix.athssox.application.security.internal;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sitionix.athssox.application.config.InternalAuthConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DevJwtServiceIdentityVerifier {

    private final InternalAuthConfig internalAuthConfig;

    private JWTVerifier jwtVerifier;

    @PostConstruct
    void init() {
        if (this.internalAuthConfig.getMode() != InternalAuthMode.DEV_JWT) {
            return;
        }
        final InternalAuthConfig.DevJwtConfig devConfig = this.internalAuthConfig.getDev();
        final Algorithm algorithm = Algorithm.HMAC256(devConfig.getJwtSecret());
        final List<String> audiences = this.getAudiences(devConfig);
        this.jwtVerifier = JWT.require(algorithm)
                .withIssuer(devConfig.getIssuer())
                .withAudience(audiences.toArray(String[]::new))
                .build();
    }

    public ServiceIdentity verify(final String token) {
        if (!StringUtils.hasText(token)) {
            throw new BadCredentialsException("Missing internal authorization token");
        }
        if (this.jwtVerifier == null) {
            throw new BadCredentialsException("Internal authorization verifier not configured");
        }
        final DecodedJWT decoded = this.verifyToken(token);
        final String subject = decoded.getSubject();
        if (!StringUtils.hasText(subject)) {
            throw new BadCredentialsException("Internal authorization token missing subject");
        }
        if (decoded.getIssuedAt() == null || decoded.getExpiresAt() == null) {
            throw new BadCredentialsException("Internal authorization token missing iat/exp");
        }
        if (decoded.getExpiresAt().toInstant().isBefore(Instant.now())) {
            throw new BadCredentialsException("Internal authorization token expired");
        }
        final List<String> audiences = decoded.getAudience();
        final String audience = audiences == null || audiences.isEmpty() ? null : audiences.get(0);
        final List<String> scopes = this.extractScopes(decoded);

        return new ServiceIdentity(subject, decoded.getIssuer(), audience, scopes);
    }

    private DecodedJWT verifyToken(final String token) {
        try {
            return this.jwtVerifier.verify(token);
        } catch (final JWTVerificationException ex) {
            throw new BadCredentialsException("Invalid internal authorization token");
        }
    }

    private List<String> extractScopes(final DecodedJWT decodedJWT) {
        final List<String> scopes = new ArrayList<>();
        final Claim scopeClaim = decodedJWT.getClaim("scope");
        if (scopeClaim != null && !scopeClaim.isNull()) {
            final String scopeValue = scopeClaim.asString();
            if (StringUtils.hasText(scopeValue)) {
                Collections.addAll(scopes, scopeValue.trim().split("\\s+"));
            } else {
                final List<String> scopeList = scopeClaim.asList(String.class);
                if (scopeList != null) {
                    scopes.addAll(scopeList);
                }
            }
        }
        if (scopes.isEmpty()) {
            final Claim scpClaim = decodedJWT.getClaim("scp");
            if (scpClaim != null && !scpClaim.isNull()) {
                final List<String> scopeList = scpClaim.asList(String.class);
                if (scopeList != null) {
                    scopes.addAll(scopeList);
                }
            }
        }
        return scopes;
    }

    private List<String> getAudiences(final InternalAuthConfig.DevJwtConfig devConfig) {
        if (devConfig.getAudience() == null || devConfig.getAudience().isEmpty()) {
            return List.of();
        }
        return devConfig.getAudience();
    }
}
