package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.TokenConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtKeyMaterialLoaderTest {

    private JwtKeyMaterialLoader jwtKeyMaterialLoader;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private Resource resource;

    @BeforeEach
    void setUp() {
        this.jwtKeyMaterialLoader = new JwtKeyMaterialLoader(this.resourceLoader);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.resourceLoader,
                this.resource);
    }

    @Test
    void givenPemConfigWithPrivateAndPublic_whenLoadActiveKey_thenReturnKeyPair() {
        //given
        final KeyPair keyPair = this.getKeyPair();
        final String privatePem = this.getPrivateKeyPem(keyPair);
        final String publicPem = this.getPublicKeyPem(keyPair);
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(privatePem, null, publicPem, null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("key-1",
                this.getKeyStoreConfig(null, null, null, null),
                pemConfig,
                List.of());

        //when
        final JwtKey actual = this.jwtKeyMaterialLoader.loadActiveKey(jwtConfig);

        //then
        final RSAPublicKey expectedPublicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAPrivateKey expectedPrivateKey = (RSAPrivateKey) keyPair.getPrivate();

        assertThat(actual.getKeyId()).isEqualTo("key-1");
        assertThat(actual.getPublicKey().getModulus()).isEqualTo(expectedPublicKey.getModulus());
        assertThat(actual.getPublicKey().getPublicExponent()).isEqualTo(expectedPublicKey.getPublicExponent());
        assertThat(actual.getPrivateKey().getModulus()).isEqualTo(expectedPrivateKey.getModulus());
        assertThat(actual.getPrivateKey().getPrivateExponent()).isEqualTo(expectedPrivateKey.getPrivateExponent());
    }

    @Test
    void givenPemConfigWithPrivateOnly_whenLoadActiveKey_thenDerivePublicKey() {
        //given
        final KeyPair keyPair = this.getKeyPair();
        final String privatePem = this.getPrivateKeyPem(keyPair);
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(privatePem, null, null, null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("key-2",
                this.getKeyStoreConfig(null, null, null, null),
                pemConfig,
                List.of());

        //when
        final JwtKey actual = this.jwtKeyMaterialLoader.loadActiveKey(jwtConfig);

        //then
        final RSAPublicKey expectedPublicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAPrivateKey expectedPrivateKey = (RSAPrivateKey) keyPair.getPrivate();

        assertThat(actual.getKeyId()).isEqualTo("key-2");
        assertThat(actual.getPublicKey().getModulus()).isEqualTo(expectedPublicKey.getModulus());
        assertThat(actual.getPublicKey().getPublicExponent()).isEqualTo(expectedPublicKey.getPublicExponent());
        assertThat(actual.getPrivateKey().getModulus()).isEqualTo(expectedPrivateKey.getModulus());
        assertThat(actual.getPrivateKey().getPrivateExponent()).isEqualTo(expectedPrivateKey.getPrivateExponent());
    }

    @Test
    void givenInlineVerificationKeys_whenLoadVerificationKeys_thenReturnKeys() {
        //given
        final KeyPair keyPair = this.getKeyPair();
        final String publicPem = this.getPublicKeyPem(keyPair);
        final KeyPair signingKeyPair = this.getKeyPair();
        final String signingPrivatePem = this.getPrivateKeyPem(signingKeyPair);
        final TokenConfig.VerificationKeyConfig verificationKey =
                this.getVerificationKeyConfig("verify-1", publicPem, null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("key-3",
                this.getKeyStoreConfig(null, null, null, null),
                this.getPemConfig(signingPrivatePem, null, null, null),
                List.of(verificationKey));

        //when
        final List<JwtKey> actual = this.jwtKeyMaterialLoader.loadVerificationKeys(jwtConfig);

        //then
        final RSAPublicKey expectedPublicKey = (RSAPublicKey) keyPair.getPublic();

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getKeyId()).isEqualTo("verify-1");
        assertThat(actual.get(0).getPublicKey().getModulus()).isEqualTo(expectedPublicKey.getModulus());
        assertThat(actual.get(0).getPublicKey().getPublicExponent()).isEqualTo(expectedPublicKey.getPublicExponent());
        assertThat(actual.get(0).getPrivateKey()).isNull();
    }

    @Test
    void givenVerificationKeyPath_whenLoadVerificationKeys_thenLoadFromResource() throws Exception {
        //given
        final KeyPair keyPair = this.getKeyPair();
        final String publicPem = this.getPublicKeyPem(keyPair);
        final KeyPair signingKeyPair = this.getKeyPair();
        final String signingPrivatePem = this.getPrivateKeyPem(signingKeyPair);
        final String resourcePath = this.getResourcePath();
        final TokenConfig.VerificationKeyConfig verificationKey =
                this.getVerificationKeyConfig("verify-2", null, resourcePath);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("key-4",
                this.getKeyStoreConfig(null, null, null, null),
                this.getPemConfig(signingPrivatePem, null, null, null),
                List.of(verificationKey));

        when(this.resourceLoader.getResource(resourcePath))
                .thenReturn(this.resource);
        when(this.resource.exists())
                .thenReturn(true);
        when(this.resource.getInputStream())
                .thenReturn(this.getInputStream(publicPem));

        //when
        final List<JwtKey> actual = this.jwtKeyMaterialLoader.loadVerificationKeys(jwtConfig);

        //then
        final RSAPublicKey expectedPublicKey = (RSAPublicKey) keyPair.getPublic();

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getKeyId()).isEqualTo("verify-2");
        assertThat(actual.get(0).getPublicKey().getModulus()).isEqualTo(expectedPublicKey.getModulus());
        assertThat(actual.get(0).getPublicKey().getPublicExponent()).isEqualTo(expectedPublicKey.getPublicExponent());
        assertThat(actual.get(0).getPrivateKey()).isNull();
        verify(this.resourceLoader)
                .getResource(resourcePath);
        verify(this.resource)
                .exists();
        verify(this.resource)
                .getInputStream();
    }

    private TokenConfig.JwtConfig getJwtConfig(final String keyId,
                                               final TokenConfig.KeyStoreConfig keyStoreConfig,
                                               final TokenConfig.PemConfig pemConfig,
                                               final List<TokenConfig.VerificationKeyConfig> verificationKeys) {
        final TokenConfig.JwtConfig jwtConfig = new TokenConfig.JwtConfig();
        jwtConfig.setKeyId(keyId);
        jwtConfig.setKeyStore(keyStoreConfig);
        jwtConfig.setPem(pemConfig);
        jwtConfig.setVerificationKeys(verificationKeys);
        return jwtConfig;
    }

    private TokenConfig.KeyStoreConfig getKeyStoreConfig(final String path,
                                                         final String password,
                                                         final String alias,
                                                         final String keyPassword) {
        final TokenConfig.KeyStoreConfig keyStoreConfig = new TokenConfig.KeyStoreConfig();
        keyStoreConfig.setPath(path);
        keyStoreConfig.setPassword(password);
        keyStoreConfig.setAlias(alias);
        keyStoreConfig.setKeyPassword(keyPassword);
        return keyStoreConfig;
    }

    private TokenConfig.PemConfig getPemConfig(final String privateKey,
                                               final String privateKeyPath,
                                               final String publicKey,
                                               final String publicKeyPath) {
        final TokenConfig.PemConfig pemConfig = new TokenConfig.PemConfig();
        pemConfig.setPrivateKey(privateKey);
        pemConfig.setPrivateKeyPath(privateKeyPath);
        pemConfig.setPublicKey(publicKey);
        pemConfig.setPublicKeyPath(publicKeyPath);
        return pemConfig;
    }

    private TokenConfig.VerificationKeyConfig getVerificationKeyConfig(final String keyId,
                                                                       final String publicKey,
                                                                       final String publicKeyPath) {
        final TokenConfig.VerificationKeyConfig verificationKeyConfig = new TokenConfig.VerificationKeyConfig();
        verificationKeyConfig.setKeyId(keyId);
        verificationKeyConfig.setPublicKey(publicKey);
        verificationKeyConfig.setPublicKeyPath(publicKeyPath);
        return verificationKeyConfig;
    }

    private String getPrivateKeyPem(final KeyPair keyPair) {
        return this.wrapPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
    }

    private String getPublicKeyPem(final KeyPair keyPair) {
        return this.wrapPem("PUBLIC KEY", keyPair.getPublic().getEncoded());
    }

    private String wrapPem(final String type, final byte[] encoded) {
        final String base64 = Base64.getEncoder().encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----";
    }

    private InputStream getInputStream(final String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.US_ASCII));
    }

    private String getResourcePath() {
        return "classpath:jwt-public.pem";
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
