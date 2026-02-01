package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.dto.JwksResponse;
import com.sitionix.athssox.api.mapper.JwksApiMapper;
import com.sitionix.athssox.domain.service.JwksProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwksControllerTest {

    private JwksController jwksController;

    @Mock
    private JwksProvider jwksProvider;

    @Mock
    private JwksApiMapper jwksApiMapper;

    @BeforeEach
    void setUp() {
        this.jwksController = new JwksController(this.jwksProvider, this.jwksApiMapper);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.jwksProvider, this.jwksApiMapper);
    }

    @Test
    void given_jwks_provider_when_get_jwks_alias_then_return_jwks_response() {
        //given
        final com.sitionix.athssox.domain.model.jwks.JwksResponse domainResponse =
                mock(com.sitionix.athssox.domain.model.jwks.JwksResponse.class);
        final JwksResponse expected = mock(JwksResponse.class);

        when(this.jwksProvider.getJwks())
                .thenReturn(domainResponse);
        when(this.jwksApiMapper.asJwksResponseDTO(domainResponse))
                .thenReturn(expected);

        //when
        final ResponseEntity<JwksResponse> actual = this.jwksController.getJwksAlias();

        //then
        assertThat(actual).isEqualTo(ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300L, TimeUnit.SECONDS).cachePublic())
                .body(expected));
        verify(this.jwksProvider)
                .getJwks();
        verify(this.jwksApiMapper)
                .asJwksResponseDTO(domainResponse);
    }

    @Test
    void given_jwks_provider_when_get_jwks_canonical_then_return_jwks_response() {
        //given
        final com.sitionix.athssox.domain.model.jwks.JwksResponse domainResponse =
                mock(com.sitionix.athssox.domain.model.jwks.JwksResponse.class);
        final JwksResponse expected = mock(JwksResponse.class);

        when(this.jwksProvider.getJwks())
                .thenReturn(domainResponse);
        when(this.jwksApiMapper.asJwksResponseDTO(domainResponse))
                .thenReturn(expected);

        //when
        final ResponseEntity<JwksResponse> actual = this.jwksController.getJwksCanonical();

        //then
        assertThat(actual).isEqualTo(ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300L, TimeUnit.SECONDS).cachePublic())
                .body(expected));
        verify(this.jwksProvider)
                .getJwks();
        verify(this.jwksApiMapper)
                .asJwksResponseDTO(domainResponse);
    }
}
