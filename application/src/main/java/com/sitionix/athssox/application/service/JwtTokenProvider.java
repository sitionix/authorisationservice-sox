package com.sitionix.athssox.application.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sitionix.athssox.application.config.TokenConfig;
import com.sitionix.athssox.domain.model.AccessToken;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.RefreshToken;
import com.sitionix.athssox.domain.service.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProvider {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final TokenConfig tokenConfig;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public AccessToken generateAccessToken(final AuthUser user) {
        final Instant now = this.clock.instant();
        final Instant expiresAt = now.plusSeconds(this.tokenConfig.getAccessTokenTtlSeconds());
        final Algorithm algorithm = Algorithm.HMAC256(this.tokenConfig.getJwtSecret());

        final String token = JWT.create()
                .withIssuer(this.tokenConfig.getIssuer())
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole().name())
                .withClaim("siteId", Optional.ofNullable(user.getSiteId())
                        .map(Object::toString)
                        .orElse(null))
                .withClaim("type", "access")
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);

        return AccessToken.builder()
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public RefreshToken generateRefreshToken(final AuthUser user) {
        final Instant now = this.clock.instant();
        final Instant expiresAt = now.plusSeconds(this.tokenConfig.getRefreshTokenTtlSeconds());
        final byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        this.secureRandom.nextBytes(randomBytes);

        final String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        return RefreshToken.builder()
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }
}
