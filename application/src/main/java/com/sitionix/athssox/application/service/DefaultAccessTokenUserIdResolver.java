package com.sitionix.athssox.application.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sitionix.athssox.application.config.TokenConfig;
import com.sitionix.athssox.domain.exception.AccessTokenAuthenticationException;
import com.sitionix.athssox.domain.service.AccessTokenUserIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DefaultAccessTokenUserIdResolver implements AccessTokenUserIdResolver {

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtKeyProvider jwtKeyProvider;
    private final TokenConfig tokenConfig;

    @Override
    public Long resolveUserId(final String accessToken) {
        final String token = this.extractToken(accessToken);
        final DecodedJWT decodedJwt = this.verifyToken(token);
        final String subject = decodedJwt.getSubject();
        if (!StringUtils.hasText(subject)) {
            throw new AccessTokenAuthenticationException("Authentication required.");
        }
        try {
            return Long.valueOf(subject);
        } catch (final NumberFormatException ex) {
            throw new AccessTokenAuthenticationException("Authentication required.");
        }
    }

    private String extractToken(final String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            throw new AccessTokenAuthenticationException("Authentication required.");
        }
        if (StringUtils.startsWithIgnoreCase(headerValue, BEARER_PREFIX)) {
            return headerValue.substring(BEARER_PREFIX.length());
        }
        return headerValue;
    }

    private DecodedJWT verifyToken(final String token) {
        try {
            final JWTVerifier verifier = JWT.require(this.jwtKeyProvider.getSigningAlgorithm())
                    .withIssuer(this.tokenConfig.getIssuer())
                    .withClaim("type", ACCESS_TOKEN_TYPE)
                    .build();
            return verifier.verify(token);
        } catch (final JWTVerificationException ex) {
            throw new AccessTokenAuthenticationException("Authentication required.");
        }
    }
}
