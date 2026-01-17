package com.sitionix.athssox.application.service;

import com.sitionix.athssox.domain.model.jwks.JwkKey;
import com.sitionix.athssox.domain.model.jwks.JwksResponse;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
class JwtJwksFactory {

    private static final String ALG_RS256 = "RS256";
    private static final String KEY_TYPE = "RSA";
    private static final String KEY_USE = "sig";

    JwksResponse build(final JwtKey activeKey, final List<JwtKey> verificationKeys) {
        final Map<String, JwtKey> keysById = new LinkedHashMap<>();
        keysById.put(activeKey.getKeyId(), activeKey);
        for (final JwtKey verificationKey : verificationKeys) {
            keysById.putIfAbsent(verificationKey.getKeyId(), verificationKey);
        }

        final List<JwkKey> jwkKeys = new ArrayList<>();
        for (final JwtKey key : keysById.values()) {
            jwkKeys.add(this.toJwkKey(key));
        }

        return JwksResponse.builder()
                .keys(jwkKeys)
                .build();
    }

    private JwkKey toJwkKey(final JwtKey key) {
        final RSAPublicKey publicKey = key.getPublicKey();
        return JwkKey.builder()
                .kty(KEY_TYPE)
                .kid(key.getKeyId())
                .use(KEY_USE)
                .alg(ALG_RS256)
                .n(this.toBase64Url(publicKey.getModulus()))
                .e(this.toBase64Url(publicKey.getPublicExponent()))
                .build();
    }

    private String toBase64Url(final BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            final byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
