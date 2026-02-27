package com.sitionix.athssox.application.service;

import com.sitionix.athssox.domain.model.jwks.JwkKey;
import com.sitionix.athssox.domain.model.jwks.JwksResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtJwksFactoryTest {

    private JwtJwksFactory jwtJwksFactory;

    @BeforeEach
    void setUp() {
        this.jwtJwksFactory = new JwtJwksFactory();
    }

    @Test
    void givenActiveAndVerificationKeys_whenBuild_thenReturnJwksResponse() {
        //given
        final KeyPair activeKeyPair = this.getKeyPair();
        final KeyPair verificationKeyPair = this.getKeyPair();
        final JwtKey activeKey = this.getJwtKey("key-1", activeKeyPair, true);
        final JwtKey verificationKey = this.getJwtKey("key-2", verificationKeyPair, false);
        final List<JwkKey> expectedKeys = List.of(
                this.getJwkKey("key-1", (RSAPublicKey) activeKeyPair.getPublic()),
                this.getJwkKey("key-2", (RSAPublicKey) verificationKeyPair.getPublic())
        );
        final JwksResponse expected = this.getJwksResponse(expectedKeys);

        //when
        final JwksResponse actual = this.jwtJwksFactory.build(activeKey, List.of(verificationKey));

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenDuplicateKeyIds_whenBuild_thenReturnUniqueKeys() {
        //given
        final KeyPair activeKeyPair = this.getKeyPair();
        final KeyPair verificationKeyPair = this.getKeyPair();
        final JwtKey activeKey = this.getJwtKey("key-1", activeKeyPair, true);
        final JwtKey verificationKey = this.getJwtKey("key-1", verificationKeyPair, false);
        final List<JwkKey> expectedKeys = List.of(
                this.getJwkKey("key-1", (RSAPublicKey) activeKeyPair.getPublic())
        );
        final JwksResponse expected = this.getJwksResponse(expectedKeys);

        //when
        final JwksResponse actual = this.jwtJwksFactory.build(activeKey, List.of(verificationKey));

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private JwtKey getJwtKey(final String keyId, final KeyPair keyPair, final boolean includePrivateKey) {
        final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAPrivateKey privateKey = includePrivateKey ? (RSAPrivateKey) keyPair.getPrivate() : null;
        return new JwtKey(keyId, publicKey, privateKey);
    }

    private JwksResponse getJwksResponse(final List<JwkKey> keys) {
        return JwksResponse.builder()
                .keys(keys)
                .build();
    }

    private JwkKey getJwkKey(final String keyId, final RSAPublicKey publicKey) {
        return JwkKey.builder()
                .kty("RSA")
                .kid(keyId)
                .use("sig")
                .alg("RS256")
                .n(this.getBase64Url(publicKey.getModulus()))
                .e(this.getBase64Url(publicKey.getPublicExponent()))
                .build();
    }

    private String getBase64Url(final BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            final byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private KeyPair getKeyPair() {
        try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair for test.", ex);
        }
    }
}
