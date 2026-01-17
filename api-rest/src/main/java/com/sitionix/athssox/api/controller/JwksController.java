package com.sitionix.athssox.api.controller;

import com.sitionix.athssox.domain.model.jwks.JwksResponse;
import com.sitionix.athssox.domain.service.JwksProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private static final long CACHE_SECONDS = 300L;

    private final JwksProvider jwksProvider;

    @GetMapping({"/.well-known/jwks.json", "/oauth2/jwks"})
    public ResponseEntity<JwksResponse> getJwks() {
        final JwksResponse response = this.jwksProvider.getJwks();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CACHE_SECONDS, TimeUnit.SECONDS).cachePublic())
                .body(response);
    }
}
