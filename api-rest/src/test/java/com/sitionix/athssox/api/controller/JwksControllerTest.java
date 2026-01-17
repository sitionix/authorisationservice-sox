package com.sitionix.athssox.api.controller;

import com.sitionix.athssox.domain.model.jwks.JwkKey;
import com.sitionix.athssox.domain.model.jwks.JwksResponse;
import com.sitionix.athssox.domain.service.JwksProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwksControllerTest {

    private JwksController jwksController;

    @Mock
    private JwksProvider jwksProvider;

    @BeforeEach
    void setUp() {
        this.jwksController = new JwksController(this.jwksProvider);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.jwksProvider);
    }

    @Test
    void givenJwksProvider_whenGetJwks_thenReturnJwksResponse() {
        //given
        final JwksResponse expected = this.getJwksResponse("key-1");

        when(this.jwksProvider.getJwks())
                .thenReturn(expected);

        //when
        final ResponseEntity<JwksResponse> actual = this.jwksController.getJwks();

        //then
        assertThat(actual).isEqualTo(ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300L, TimeUnit.SECONDS).cachePublic())
                .body(expected));
        verify(this.jwksProvider)
                .getJwks();
    }

    private JwksResponse getJwksResponse(final String keyId) {
        return JwksResponse.builder()
                .keys(List.of(this.getJwkKey(keyId)))
                .build();
    }

    private JwkKey getJwkKey(final String keyId) {
        return JwkKey.builder()
                .kty("RSA")
                .kid(keyId)
                .use("sig")
                .alg("RS256")
                .n("modulus")
                .e("exponent")
                .build();
    }
}
