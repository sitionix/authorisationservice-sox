package com.sitionix.athssox.application.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sitionix.athssox.application.config.TokenConfig;
import com.sitionix.athssox.domain.exception.AccessTokenAuthenticationException;
import com.sitionix.athssox.domain.service.AccessTokenUserIdResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAccessTokenUserIdResolverTest {

    private AccessTokenUserIdResolver accessTokenUserIdResolver;

    @Mock
    private JwtKeyProvider jwtKeyProvider;

    @Mock
    private TokenConfig tokenConfig;

    @BeforeEach
    void setUp() {
        this.accessTokenUserIdResolver = new DefaultAccessTokenUserIdResolver(this.jwtKeyProvider, this.tokenConfig);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.jwtKeyProvider, this.tokenConfig);
    }

    @Test
    void givenValidAccessToken_whenResolveUserId_thenReturnUserId() {
        //given
        final Algorithm algorithm = Algorithm.HMAC256("secret");
        final String issuer = "issuer";
        final String token = JWT.create()
                .withIssuer(issuer)
                .withSubject("42")
                .withClaim("type", "access")
                .sign(algorithm);

        when(this.jwtKeyProvider.getSigningAlgorithm())
                .thenReturn(algorithm);
        when(this.tokenConfig.getIssuer())
                .thenReturn(issuer);

        //when
        final Long actual = this.accessTokenUserIdResolver.resolveUserId("Bearer " + token);

        //then
        assertThat(actual).isEqualTo(42L);
        verify(this.jwtKeyProvider).getSigningAlgorithm();
        verify(this.tokenConfig).getIssuer();
    }

    @Test
    void givenMissingAccessToken_whenResolveUserId_thenThrowException() {
        //given
        final String accessToken = null;

        //when
        final Throwable actual = catchThrowable(() -> this.accessTokenUserIdResolver.resolveUserId(accessToken));

        //then
        assertThat(actual).isInstanceOf(AccessTokenAuthenticationException.class);
    }
}
