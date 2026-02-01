package com.sitionix.athssox.api.controller;

import com.app_afesox.athssox.api_first.api.SecurityApi;
import com.app_afesox.athssox.api_first.dto.JwksResponse;
import com.sitionix.athssox.api.mapper.JwksApiMapper;
import com.sitionix.athssox.domain.service.JwksProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class JwksController implements SecurityApi {

    private static final long CACHE_SECONDS = 300L;

    private final JwksProvider jwksProvider;

    private final JwksApiMapper jwksApiMapper;

    @Override
    public ResponseEntity<JwksResponse> getJwksAlias() {
        return this.buildJwksResponse();
    }

    @Override
    public ResponseEntity<JwksResponse> getJwksCanonical() {
        return this.buildJwksResponse();
    }

    private ResponseEntity<JwksResponse> buildJwksResponse() {
        final com.sitionix.athssox.domain.model.jwks.JwksResponse response = this.jwksProvider.getJwks();
        final JwksResponse mapped = this.jwksApiMapper.asJwksResponseDTO(response);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CACHE_SECONDS, TimeUnit.SECONDS).cachePublic())
                .body(mapped);
    }
}
