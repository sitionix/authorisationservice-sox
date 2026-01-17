package com.sitionix.athssox.application.service;

import com.auth0.jwt.algorithms.Algorithm;
import com.sitionix.athssox.application.config.TokenConfig;
import com.sitionix.athssox.domain.model.jwks.JwkKey;
import com.sitionix.athssox.domain.model.jwks.JwksResponse;
import com.sitionix.athssox.domain.service.JwksProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtKeyProvider implements JwksProvider {

    private static final String ALG_RS256 = "RS256";
    private static final String KEY_TYPE = "RSA";
    private static final String KEY_USE = "sig";

    private final TokenConfig tokenConfig;
    private final ResourceLoader resourceLoader;

    private JwtKey activeKey;
    private List<JwtKey> verificationKeys = List.of();
    private Algorithm signingAlgorithm;

    @PostConstruct
    void init() {
        final TokenConfig.JwtConfig jwtConfig = this.tokenConfig.getJwt();
        this.validateJwtConfig(jwtConfig);

        this.activeKey = this.loadActiveKey(jwtConfig);
        this.verificationKeys = this.loadVerificationKeys(jwtConfig);
        this.signingAlgorithm = Algorithm.RSA256(this.activeKey.publicKey, this.activeKey.privateKey);
    }

    public Algorithm getSigningAlgorithm() {
        return this.signingAlgorithm;
    }

    public String getActiveKeyId() {
        return this.activeKey.keyId;
    }

    @Override
    public JwksResponse getJwks() {
        final Map<String, JwtKey> keysById = new LinkedHashMap<>();
        keysById.put(this.activeKey.keyId, this.activeKey);
        for (final JwtKey verificationKey : this.verificationKeys) {
            keysById.putIfAbsent(verificationKey.keyId, verificationKey);
        }

        final List<JwkKey> jwkKeys = new ArrayList<>();
        for (final JwtKey key : keysById.values()) {
            jwkKeys.add(this.toJwkKey(key));
        }

        return JwksResponse.builder()
                .keys(jwkKeys)
                .build();
    }

    private void validateJwtConfig(final TokenConfig.JwtConfig jwtConfig) {
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

    private JwtKey loadActiveKey(final TokenConfig.JwtConfig jwtConfig) {
        if (StringUtils.hasText(jwtConfig.getKeyStore().getPath())) {
            return this.loadKeyPairFromKeyStore(jwtConfig);
        }
        return this.loadKeyPairFromPem(jwtConfig);
    }

    private List<JwtKey> loadVerificationKeys(final TokenConfig.JwtConfig jwtConfig) {
        final List<TokenConfig.VerificationKeyConfig> verificationKeys = this.getVerificationKeys(jwtConfig);
        if (verificationKeys.isEmpty()) {
            return List.of();
        }

        final List<JwtKey> keys = new ArrayList<>();
        for (final TokenConfig.VerificationKeyConfig verificationKey : verificationKeys) {
            final RSAPublicKey publicKey = this.loadPublicKey(verificationKey.getPublicKey(),
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
        RSAPublicKey publicKey = this.loadPublicKey(jwtConfig.getPem().getPublicKey(),
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

    private RSAPublicKey loadPublicKey(final String publicKeyInline,
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

    private JwkKey toJwkKey(final JwtKey key) {
        final RSAPublicKey publicKey = key.publicKey;
        return JwkKey.builder()
                .kty(KEY_TYPE)
                .kid(key.keyId)
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

    private static final class JwtKey {

        private final String keyId;
        private final RSAPublicKey publicKey;
        private final RSAPrivateKey privateKey;

        private JwtKey(final String keyId,
                       final RSAPublicKey publicKey,
                       final RSAPrivateKey privateKey) {
            this.keyId = keyId;
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
    }
}
