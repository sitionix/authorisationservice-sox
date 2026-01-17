package com.sitionix.athssox.application.service;

import com.auth0.jwt.algorithms.Algorithm;
import com.sitionix.athssox.application.config.TokenConfig;
import com.sitionix.athssox.domain.model.jwks.JwksResponse;
import com.sitionix.athssox.domain.service.JwksProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtKeyProvider implements JwksProvider {

    private final TokenConfig tokenConfig;
    private final JwtKeyConfigValidator configValidator;
    private final JwtKeyMaterialLoader keyMaterialLoader;
    private final JwtJwksFactory jwksFactory;

    private JwtKey activeKey;
    private List<JwtKey> verificationKeys = List.of();
    private Algorithm signingAlgorithm;

    @PostConstruct
    void init() {
        final TokenConfig.JwtConfig jwtConfig = this.tokenConfig.getJwt();
        this.configValidator.validate(jwtConfig);
        this.activeKey = this.keyMaterialLoader.loadActiveKey(jwtConfig);
        this.verificationKeys = this.keyMaterialLoader.loadVerificationKeys(jwtConfig);
        this.signingAlgorithm = Algorithm.RSA256(this.activeKey.getPublicKey(), this.activeKey.getPrivateKey());
    }

    public Algorithm getSigningAlgorithm() {
        return this.signingAlgorithm;
    }

    public String getActiveKeyId() {
        return this.activeKey.getKeyId();
    }

    @Override
    public JwksResponse getJwks() {
        return this.jwksFactory.build(this.activeKey, this.verificationKeys);
    }
}
