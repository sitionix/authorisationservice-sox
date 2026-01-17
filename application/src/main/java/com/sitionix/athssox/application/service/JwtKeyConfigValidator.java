package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.TokenConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
class JwtKeyConfigValidator {

    private static final String ALG_RS256 = "RS256";

    void validate(final TokenConfig.JwtConfig jwtConfig) {
        if (!StringUtils.hasText(jwtConfig.getAlg())
                || !ALG_RS256.equalsIgnoreCase(jwtConfig.getAlg())) {
            throw new IllegalStateException("Only RS256 JWT signing is supported.");
        }
        if (!StringUtils.hasText(jwtConfig.getKeyId())) {
            throw new IllegalStateException("JWT key id (kid) must be configured.");
        }

        final boolean hasKeyStore = StringUtils.hasText(jwtConfig.getKeyStore().getPath());
        final boolean hasPemPrivate = StringUtils.hasText(jwtConfig.getPem().getPrivateKey())
                || StringUtils.hasText(jwtConfig.getPem().getPrivateKeyPath());

        if (hasKeyStore == hasPemPrivate) {
            throw new IllegalStateException("Configure either a key store or a PEM private key for JWT signing.");
        }

        if (hasKeyStore) {
            if (!StringUtils.hasText(jwtConfig.getKeyStore().getAlias())
                    || !StringUtils.hasText(jwtConfig.getKeyStore().getPassword())) {
                throw new IllegalStateException("JWT key store path, alias and password must be configured.");
            }
        }

        for (final TokenConfig.VerificationKeyConfig verificationKey : this.getVerificationKeys(jwtConfig)) {
            if (!StringUtils.hasText(verificationKey.getKeyId())
                    || (!StringUtils.hasText(verificationKey.getPublicKey())
                    && !StringUtils.hasText(verificationKey.getPublicKeyPath()))) {
                throw new IllegalStateException("Each verification key must define kid and public key material.");
            }
        }
    }

    private List<TokenConfig.VerificationKeyConfig> getVerificationKeys(final TokenConfig.JwtConfig jwtConfig) {
        if (jwtConfig.getVerificationKeys() == null) {
            return List.of();
        }
        return jwtConfig.getVerificationKeys();
    }
}
