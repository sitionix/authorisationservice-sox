package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.TokenConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
class JwtKeyMaterialLoader {

    private final ResourceLoader resourceLoader;

    JwtKey loadActiveKey(final TokenConfig.JwtConfig jwtConfig) {
        if (StringUtils.hasText(jwtConfig.getKeyStore().getPath())) {
            return this.loadKeyPairFromKeyStore(jwtConfig);
        }
        return this.loadKeyPairFromPem(jwtConfig);
    }

    List<JwtKey> loadVerificationKeys(final TokenConfig.JwtConfig jwtConfig) {
        final List<TokenConfig.VerificationKeyConfig> verificationKeys = this.getVerificationKeys(jwtConfig);
        if (verificationKeys.isEmpty()) {
            return List.of();
        }

        final List<JwtKey> keys = new ArrayList<>();
        for (final TokenConfig.VerificationKeyConfig verificationKey : verificationKeys) {
            final RSAPublicKey publicKey = this.loadRequiredPublicKey(verificationKey.getPublicKey(),
                    verificationKey.getPublicKeyPath(),
                    "verification key");
            keys.add(new JwtKey(verificationKey.getKeyId(), publicKey, null));
        }
        return Collections.unmodifiableList(keys);
    }

    private List<TokenConfig.VerificationKeyConfig> getVerificationKeys(final TokenConfig.JwtConfig jwtConfig) {
        if (jwtConfig.getVerificationKeys() == null) {
            return List.of();
        }
        return jwtConfig.getVerificationKeys();
    }

    private JwtKey loadKeyPairFromKeyStore(final TokenConfig.JwtConfig jwtConfig) {
        final TokenConfig.KeyStoreConfig keyStoreConfig = jwtConfig.getKeyStore();
        final char[] storePassword = keyStoreConfig.getPassword().toCharArray();
        final char[] keyPassword = StringUtils.hasText(keyStoreConfig.getKeyPassword())
                ? keyStoreConfig.getKeyPassword().toCharArray()
                : storePassword;

        try (InputStream inputStream = this.openResource(keyStoreConfig.getPath())) {
            final KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(inputStream, storePassword);

            if (!keyStore.containsAlias(keyStoreConfig.getAlias())) {
                throw new IllegalStateException("JWT key alias not found in key store.");
            }

            final Key key = keyStore.getKey(keyStoreConfig.getAlias(), keyPassword);
            if (!(key instanceof RSAPrivateKey)) {
                throw new IllegalStateException("JWT key store entry must contain an RSA private key.");
            }

            final Certificate certificate = keyStore.getCertificate(keyStoreConfig.getAlias());
            if (certificate == null) {
                throw new IllegalStateException("JWT key store certificate not found for alias.");
            }
            final PublicKey publicKey = certificate.getPublicKey();
            if (!(publicKey instanceof RSAPublicKey)) {
                throw new IllegalStateException("JWT key store entry must contain an RSA public key.");
            }

            return new JwtKey(jwtConfig.getKeyId(), (RSAPublicKey) publicKey, (RSAPrivateKey) key);
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed to load JWT keys from key store.", ex);
        }
    }

    private JwtKey loadKeyPairFromPem(final TokenConfig.JwtConfig jwtConfig) {
        final RSAPrivateKey privateKey = this.loadPrivateKey(jwtConfig);
        RSAPublicKey publicKey = this.loadOptionalPublicKey(jwtConfig.getPem().getPublicKey(),
                jwtConfig.getPem().getPublicKeyPath(),
                "JWT public key");

        if (publicKey == null) {
            publicKey = this.derivePublicKey(privateKey);
        }

        return new JwtKey(jwtConfig.getKeyId(), publicKey, privateKey);
    }

    private RSAPrivateKey loadPrivateKey(final TokenConfig.JwtConfig jwtConfig) {
        final String privatePem = this.loadKeyMaterial(jwtConfig.getPem().getPrivateKey(),
                jwtConfig.getPem().getPrivateKeyPath(),
                "JWT private key");
        return this.parsePrivateKey(privatePem);
    }

    private RSAPublicKey loadRequiredPublicKey(final String publicKeyInline,
                                               final String publicKeyPath,
                                               final String label) {
        final String publicPem = this.loadKeyMaterial(publicKeyInline, publicKeyPath, label);
        return this.parsePublicKey(publicPem);
    }

    private RSAPublicKey loadOptionalPublicKey(final String publicKeyInline,
                                               final String publicKeyPath,
                                               final String label) {
        if (!StringUtils.hasText(publicKeyInline) && !StringUtils.hasText(publicKeyPath)) {
            return null;
        }

        final String publicPem = this.loadKeyMaterial(publicKeyInline, publicKeyPath, label);
        return this.parsePublicKey(publicPem);
    }

    private RSAPublicKey derivePublicKey(final RSAPrivateKey privateKey) {
        if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
            throw new IllegalStateException("JWT public key must be configured when private key lacks CRT data.");
        }

        try {
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final RSAPublicKeySpec keySpec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed to derive JWT public key from private key.", ex);
        }
    }

    private String loadKeyMaterial(final String inlineValue, final String path, final String label) {
        if (StringUtils.hasText(inlineValue)) {
            return this.normalizePem(inlineValue);
        }
        if (StringUtils.hasText(path)) {
            return this.readResourceToString(path);
        }
        throw new IllegalStateException(label + " must be configured.");
    }

    private RSAPrivateKey parsePrivateKey(final String pem) {
        if (pem.contains("BEGIN RSA PRIVATE KEY")) {
            throw new IllegalStateException("RSA PRIVATE KEY format is not supported; use PKCS#8 private key.");
        }

        try {
            final byte[] decoded = this.decodePem(pem);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            final PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            if (!(privateKey instanceof RSAPrivateKey)) {
                throw new IllegalStateException("JWT private key must be RSA.");
            }
            return (RSAPrivateKey) privateKey;
        } catch (final InvalidKeySpecException | java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to parse JWT private key.", ex);
        }
    }

    private RSAPublicKey parsePublicKey(final String pem) {
        try {
            final byte[] decoded = this.decodePem(pem);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            final PublicKey publicKey = keyFactory.generatePublic(keySpec);
            if (!(publicKey instanceof RSAPublicKey)) {
                throw new IllegalStateException("JWT public key must be RSA.");
            }
            return (RSAPublicKey) publicKey;
        } catch (final InvalidKeySpecException | java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to parse JWT public key.", ex);
        }
    }

    private byte[] decodePem(final String pem) {
        final String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private String normalizePem(final String pem) {
        if (pem.contains("\\n") && !pem.contains("\n")) {
            return pem.replace("\\n", "\n");
        }
        return pem;
    }

    private String readResourceToString(final String location) {
        try (InputStream inputStream = this.openResource(location)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.US_ASCII);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to read " + location + ".", ex);
        }
    }

    private InputStream openResource(final String location) throws IOException {
        final Resource resource;
        if (location.startsWith("classpath:") || location.startsWith("file:")) {
            resource = this.resourceLoader.getResource(location);
        } else {
            resource = new FileSystemResource(location);
        }

        if (!resource.exists()) {
            throw new IllegalStateException("JWT key resource not found: " + location);
        }
        return resource.getInputStream();
    }
}
