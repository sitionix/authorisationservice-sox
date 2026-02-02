package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.api.SecurityApi;
import com.app_afesox.athssox.api_first.dto.JwksResponseDTO;
import com.sitionix.athssox.api.config.JwksConfig;
import com.sitionix.athssox.api.mapper.JwksApiMapper;
import com.sitionix.athssox.domain.model.jwks.JwksResponse;
import com.sitionix.athssox.domain.service.JwksProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class JwksController implements SecurityApi {

    private final JwksProvider jwksProvider;

    private final JwksApiMapper jwksApiMapper;

    private final JwksConfig jwksConfig;

    @Override
    public ResponseEntity<JwksResponseDTO> getJwksAlias() {
        return this.buildJwksResponse();
    }

    @Override
    public ResponseEntity<JwksResponseDTO> getJwksCanonical() {
        return this.buildJwksResponse();
    }

    private ResponseEntity<JwksResponseDTO> buildJwksResponse() {
        final JwksResponse response = this.jwksProvider.getJwks();
        final JwksResponseDTO mapped = this.jwksApiMapper.asJwksResponseDTO(response);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(this.jwksConfig.getCacheSeconds(), TimeUnit.SECONDS).cachePublic())
                .body(mapped);
    }
}
