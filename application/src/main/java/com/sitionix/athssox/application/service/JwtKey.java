package com.sitionix.athssox.application.service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

class JwtKey {

    private final String keyId;
    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    JwtKey(final String keyId, final RSAPublicKey publicKey, final RSAPrivateKey privateKey) {
        this.keyId = keyId;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    String getKeyId() {
        return this.keyId;
    }

    RSAPublicKey getPublicKey() {
        return this.publicKey;
    }

    RSAPrivateKey getPrivateKey() {
        return this.privateKey;
    }
}
