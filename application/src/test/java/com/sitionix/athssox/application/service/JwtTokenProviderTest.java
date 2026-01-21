package com.sitionix.athssox.application.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sitionix.athssox.application.config.TokenConfig;
import com.sitionix.athssox.domain.model.AccessToken;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.RefreshToken;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.model.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private static final Instant NOW = Instant.parse("2024-05-01T10:15:30Z");

    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenConfig tokenConfig;

    @Mock
    private Clock clock;

    @Mock
    private JwtKeyProvider jwtKeyProvider;

    @BeforeEach
    void setUp() {
        this.jwtTokenProvider = new JwtTokenProvider(this.tokenConfig,
                this.clock,
                this.jwtKeyProvider);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.tokenConfig,
                this.clock,
                this.jwtKeyProvider);
    }

    @Test
    void given_auth_user_when_generate_access_token_then_return_access_token() {
        //given
        final AuthUser given = this.getAuthUser(42L, UUID.fromString("2d259c34-2e92-4e20-95b2-8321b2d1cb9b"));
        final Instant expiresAt = NOW.plusSeconds(3600L);
        final KeyPair keyPair = this.getRsaKeyPair();
        final String keyId = "key-1";
        final AccessToken expected = this.getAccessToken(given, expiresAt, "auth-issuer", keyId, keyPair);
        final Algorithm algorithm = this.getRsaAlgorithm(keyPair);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenConfig.getAccessTokenTtlSeconds())
                .thenReturn(3600L);
        when(this.tokenConfig.getIssuer())
                .thenReturn("auth-issuer");
        when(this.jwtKeyProvider.getSigningAlgorithm())
                .thenReturn(algorithm);
        when(this.jwtKeyProvider.getActiveKeyId())
                .thenReturn(keyId);

        //when
        final AccessToken actual = this.jwtTokenProvider.generateAccessToken(given);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.clock)
                .instant();
        verify(this.tokenConfig)
                .getAccessTokenTtlSeconds();
        verify(this.tokenConfig)
                .getIssuer();
        verify(this.jwtKeyProvider)
                .getSigningAlgorithm();
        verify(this.jwtKeyProvider)
                .getActiveKeyId();
    }

    @Test
    void given_access_token_when_verify_with_wrong_issuer_then_throw_exception() {
        //given
        final AuthUser given = this.getAuthUser(7L, UUID.randomUUID());
        final KeyPair keyPair = this.getRsaKeyPair();
        final Algorithm algorithm = this.getRsaAlgorithm(keyPair);
        final String keyId = "key-1";

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenConfig.getAccessTokenTtlSeconds())
                .thenReturn(3600L);
        when(this.tokenConfig.getIssuer())
                .thenReturn("auth-issuer");
        when(this.jwtKeyProvider.getSigningAlgorithm())
                .thenReturn(algorithm);
        when(this.jwtKeyProvider.getActiveKeyId())
                .thenReturn(keyId);

        final AccessToken accessToken = this.jwtTokenProvider.generateAccessToken(given);

        //when
        final Throwable actualThrowable = catchThrowable(() -> JWT.require(algorithm)
                .withIssuer("invalid-issuer")
                .build()
                .verify(accessToken.getToken()));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(JWTVerificationException.class);
        verify(this.clock)
                .instant();
        verify(this.tokenConfig)
                .getAccessTokenTtlSeconds();
        verify(this.tokenConfig)
                .getIssuer();
        verify(this.jwtKeyProvider)
                .getSigningAlgorithm();
        verify(this.jwtKeyProvider)
                .getActiveKeyId();
    }

    @Test
    void given_access_token_when_verify_with_wrong_audience_then_throw_exception() {
        //given
        final AuthUser given = this.getAuthUser(7L, UUID.randomUUID());
        final KeyPair keyPair = this.getRsaKeyPair();
        final Algorithm algorithm = this.getRsaAlgorithm(keyPair);
        final String keyId = "key-1";

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenConfig.getAccessTokenTtlSeconds())
                .thenReturn(3600L);
        when(this.tokenConfig.getIssuer())
                .thenReturn("auth-issuer");
        when(this.jwtKeyProvider.getSigningAlgorithm())
                .thenReturn(algorithm);
        when(this.jwtKeyProvider.getActiveKeyId())
                .thenReturn(keyId);

        final AccessToken accessToken = this.jwtTokenProvider.generateAccessToken(given);

        //when
        final Throwable actualThrowable = catchThrowable(() -> JWT.require(algorithm)
                .withIssuer("auth-issuer")
                .withAudience("invalid-audience")
                .build()
                .verify(accessToken.getToken()));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(JWTVerificationException.class);
        verify(this.clock)
                .instant();
        verify(this.tokenConfig)
                .getAccessTokenTtlSeconds();
        verify(this.tokenConfig)
                .getIssuer();
        verify(this.jwtKeyProvider)
                .getSigningAlgorithm();
        verify(this.jwtKeyProvider)
                .getActiveKeyId();
    }

    @Test
    void given_none_algorithm_token_when_verify_with_rsa_algorithm_then_throw_exception() {
        //given
        final AuthUser given = this.getAuthUser(7L, UUID.randomUUID());
        final KeyPair keyPair = this.getRsaKeyPair();
        final Algorithm algorithm = this.getRsaAlgorithm(keyPair);
        final String token = this.getNoneAlgorithmToken(given, NOW, NOW.plusSeconds(3600L), "auth-issuer");

        //when
        final Throwable actualThrowable = catchThrowable(() -> JWT.require(algorithm)
                .withIssuer("auth-issuer")
                .build()
                .verify(token));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void given_auth_user_when_generate_refresh_token_then_return_refresh_token() {
        //given
        final AuthUser given = this.getAuthUser(7L, null);
        final Instant expiresAt = NOW.plusSeconds(7200L);

        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.tokenConfig.getRefreshTokenTtlSeconds())
                .thenReturn(7200L);

        //when
        final RefreshToken actual = this.jwtTokenProvider.generateRefreshToken(given);

        //then
        assertThat(actual.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(actual.getToken()).isNotBlank();
        assertThat(actual.getToken()).hasSize(43);
        verify(this.clock)
                .instant();
        verify(this.tokenConfig)
                .getRefreshTokenTtlSeconds();
    }

    private AccessToken getAccessToken(final AuthUser user,
                                       final Instant expiresAt,
        final String issuer,
        final String keyId,
        final KeyPair keyPair) {
        final Algorithm algorithm = this.getRsaAlgorithm(keyPair);
        final String token = JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole().name())
                .withClaim("siteId", Optional.ofNullable(user.getSiteId())
                        .map(Object::toString)
                        .orElse(null))
                .withClaim("type", "access")
                .withIssuedAt(Date.from(NOW))
                .withExpiresAt(Date.from(expiresAt))
                .withKeyId(keyId)
                .sign(algorithm);

        return AccessToken.builder()
                .token(token)
                .expiresAt(expiresAt)
                .build();
    }

    private AuthUser getAuthUser(final Long id, final UUID siteId) {
        return AuthUser.builder()
                .id(id)
                .email("user@sitionix.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .role(UserRole.SITE_ADMIN)
                .siteId(siteId)
                .build();
    }

    private KeyPair getRsaKeyPair() {
        try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair for test.", ex);
        }
    }

    private Algorithm getRsaAlgorithm(final KeyPair keyPair) {
        return Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(),
                (RSAPrivateKey) keyPair.getPrivate());
    }

    private String getNoneAlgorithmToken(final AuthUser user,
                                         final Instant issuedAt,
                                         final Instant expiresAt,
                                         final String issuer) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole().name())
                .withClaim("siteId", Optional.ofNullable(user.getSiteId())
                        .map(Object::toString)
                        .orElse(null))
                .withClaim("type", "access")
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .sign(Algorithm.none());
    }
}
