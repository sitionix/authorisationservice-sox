package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.dto.JwksResponseDTO;
import com.sitionix.athssox.api.config.JwksConfig;
import com.sitionix.athssox.api.mapper.JwksApiMapper;
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

    @Mock
    private JwksConfig jwksConfig;

    @BeforeEach
    void setUp() {
        this.jwksController = new JwksController(this.jwksProvider, this.jwksApiMapper, this.jwksConfig);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.jwksProvider, this.jwksApiMapper, this.jwksConfig);
    }

    @Test
    void givenJwksProvider_whenGetJwksAlias_thenReturnJwksResponse() {
        //given
        final JwksResponse domainResponse = mock(JwksResponse.class);
        final JwksResponseDTO expected = mock(JwksResponseDTO.class);

        final long cacheSeconds = 15L;

        when(this.jwksProvider.getJwks())
                .thenReturn(domainResponse);
        when(this.jwksApiMapper.asJwksResponseDTO(domainResponse))
                .thenReturn(expected);
        when(this.jwksConfig.getCacheSeconds())
                .thenReturn(cacheSeconds);

        //when
        final ResponseEntity<JwksResponseDTO> actual = this.jwksController.getJwksAlias();

        //then
        assertThat(actual).isEqualTo(ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS).cachePublic())
                .body(expected));
        verify(this.jwksProvider)
                .getJwks();
        verify(this.jwksApiMapper)
                .asJwksResponseDTO(domainResponse);
        verify(this.jwksConfig)
                .getCacheSeconds();
    }

    @Test
    void givenJwksProvider_whenGetJwksCanonical_thenReturnJwksResponse() {
        //given
        final JwksResponse domainResponse = mock(JwksResponse.class);
        final JwksResponseDTO expected = mock(JwksResponseDTO.class);

        final long cacheSeconds = 15L;

        when(this.jwksProvider.getJwks())
                .thenReturn(domainResponse);
        when(this.jwksApiMapper.asJwksResponseDTO(domainResponse))
                .thenReturn(expected);
        when(this.jwksConfig.getCacheSeconds())
                .thenReturn(cacheSeconds);

        //when
        final ResponseEntity<JwksResponseDTO> actual = this.jwksController.getJwksCanonical();

        //then
        assertThat(actual).isEqualTo(ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS).cachePublic())
                .body(expected));
        verify(this.jwksProvider)
                .getJwks();
        verify(this.jwksApiMapper)
                .asJwksResponseDTO(domainResponse);
        verify(this.jwksConfig)
                .getCacheSeconds();
    }
}
