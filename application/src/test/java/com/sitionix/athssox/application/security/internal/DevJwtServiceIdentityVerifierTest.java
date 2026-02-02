package com.sitionix.athssox.application.security.internal;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sitionix.athssox.application.config.InternalAuthConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(MockitoExtension.class)
class DevJwtServiceIdentityVerifierTest {

    private DevJwtServiceIdentityVerifier devJwtServiceIdentityVerifier;

    @BeforeEach
    void setUp() {
        final InternalAuthConfig internalAuthConfig = this.getInternalAuthConfig();
        this.devJwtServiceIdentityVerifier = new DevJwtServiceIdentityVerifier(internalAuthConfig);
        this.devJwtServiceIdentityVerifier.init();
    }

    @Test
    void given_valid_token_when_verify_then_return_identity() {
        //given
        final Instant now = Instant.now();
        final String token = this.getToken("sitionix-internal", "notificationservice-sox",
                "athssox", "dev-secret", now, now.plusSeconds(300), "email.verify.link.issue");

        //when
        final ServiceIdentity actual = this.devJwtServiceIdentityVerifier.verify(token);

        //then
        assertThat(actual.serviceName()).isEqualTo("notificationservice-sox");
        assertThat(actual.issuer()).isEqualTo("sitionix-internal");
        assertThat(actual.audience()).isEqualTo("athssox");
        assertThat(actual.scopes()).containsExactly("email.verify.link.issue");
    }

    @Test
    void given_token_missing_exp_when_verify_then_throw_bad_credentials_exception() {
        //given
        final Instant now = Instant.now();
        final String token = this.getTokenWithoutExp("sitionix-internal", "notificationservice-sox",
                "athssox", "dev-secret", now, "email.verify.link.issue");

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.devJwtServiceIdentityVerifier.verify(token));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Internal authorization token missing iat/exp");
    }

    @Test
    void given_wrong_audience_when_verify_then_throw_bad_credentials_exception() {
        //given
        final Instant now = Instant.now();
        final String token = this.getToken("sitionix-internal", "notificationservice-sox",
                "wrong-audience", "dev-secret", now, now.plusSeconds(300), "email.verify.link.issue");

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.devJwtServiceIdentityVerifier.verify(token));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid internal authorization token");
    }

    @Test
    void given_invalid_signature_when_verify_then_throw_bad_credentials_exception() {
        //given
        final Instant now = Instant.now();
        final String token = this.getToken("sitionix-internal", "notificationservice-sox",
                "athssox", "wrong-secret", now, now.plusSeconds(300), "email.verify.link.issue");

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.devJwtServiceIdentityVerifier.verify(token));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid internal authorization token");
    }

    private InternalAuthConfig getInternalAuthConfig() {
        final InternalAuthConfig config = new InternalAuthConfig();
        config.setMode(InternalAuthMode.DEV_JWT);
        config.setServiceName("athssox");
        final InternalAuthConfig.DevJwtConfig devJwtConfig = new InternalAuthConfig.DevJwtConfig();
        devJwtConfig.setJwtSecret("dev-secret");
        devJwtConfig.setIssuer("sitionix-internal");
        devJwtConfig.setAcceptedAudiences(List.of("athssox"));
        config.setDev(devJwtConfig);
        return config;
    }

    private String getToken(final String issuer,
                            final String subject,
                            final String audience,
                            final String secret,
                            final Instant issuedAt,
                            final Instant expiresAt,
                            final String scope) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(subject)
                .withAudience(audience)
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .withClaim("scope", scope)
                .sign(Algorithm.HMAC256(secret));
    }

    private String getTokenWithoutExp(final String issuer,
                                      final String subject,
                                      final String audience,
                                      final String secret,
                                      final Instant issuedAt,
                                      final String scope) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(subject)
                .withAudience(audience)
                .withIssuedAt(Date.from(issuedAt))
                .withClaim("scope", scope)
                .sign(Algorithm.HMAC256(secret));
    }
}
