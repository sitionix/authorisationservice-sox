package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.TokenConfig;
import com.sitionix.athssox.domain.model.jwks.JwksResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtKeyProviderTest {

    private JwtKeyProvider jwtKeyProvider;

    @Mock
    private TokenConfig tokenConfig;

    @Mock
    private JwtKeyConfigValidator configValidator;

    @Mock
    private JwtKeyMaterialLoader keyMaterialLoader;

    @Mock
    private JwtJwksFactory jwksFactory;

    @BeforeEach
    void setUp() {
        this.jwtKeyProvider = new JwtKeyProvider(this.tokenConfig,
                this.configValidator,
                this.keyMaterialLoader,
                this.jwksFactory);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.tokenConfig,
                this.configValidator,
                this.keyMaterialLoader,
                this.jwksFactory);
    }

    @Test
    void givenJwtConfig_whenInit_thenPrepareSigningAlgorithmAndKeyId() {
        //given
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig();
        final JwtKey activeKey = this.getJwtKey("key-1");
        final List<JwtKey> verificationKeys = this.getVerificationKeys();

        when(this.tokenConfig.getJwt())
                .thenReturn(jwtConfig);
        when(this.keyMaterialLoader.loadActiveKey(jwtConfig))
                .thenReturn(activeKey);
        when(this.keyMaterialLoader.loadVerificationKeys(jwtConfig))
                .thenReturn(verificationKeys);

        //when
        this.jwtKeyProvider.init();
        final String actualKeyId = this.jwtKeyProvider.getActiveKeyId();
        final String actualAlgorithm = this.jwtKeyProvider.getSigningAlgorithm().getName();

        //then
        assertThat(actualKeyId).isEqualTo("key-1");
        assertThat(actualAlgorithm).isEqualTo("RS256");
        verify(this.tokenConfig)
                .getJwt();
        verify(this.configValidator)
                .validate(jwtConfig);
        verify(this.keyMaterialLoader)
                .loadActiveKey(jwtConfig);
        verify(this.keyMaterialLoader)
                .loadVerificationKeys(jwtConfig);
    }

    @Test
    void givenInitializedProvider_whenGetJwks_thenDelegateToFactory() {
        //given
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig();
        final JwtKey activeKey = this.getJwtKey("key-2");
        final List<JwtKey> verificationKeys = this.getVerificationKeys();
        final JwksResponse expected = this.getJwksResponse();

        when(this.tokenConfig.getJwt())
                .thenReturn(jwtConfig);
        when(this.keyMaterialLoader.loadActiveKey(jwtConfig))
                .thenReturn(activeKey);
        when(this.keyMaterialLoader.loadVerificationKeys(jwtConfig))
                .thenReturn(verificationKeys);
        when(this.jwksFactory.build(activeKey, verificationKeys))
                .thenReturn(expected);

        this.jwtKeyProvider.init();

        //when
        final JwksResponse actual = this.jwtKeyProvider.getJwks();

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.tokenConfig)
                .getJwt();
        verify(this.configValidator)
                .validate(jwtConfig);
        verify(this.keyMaterialLoader)
                .loadActiveKey(jwtConfig);
        verify(this.keyMaterialLoader)
                .loadVerificationKeys(jwtConfig);
        verify(this.jwksFactory)
                .build(activeKey, verificationKeys);
    }

    private TokenConfig.JwtConfig getJwtConfig() {
        final TokenConfig.JwtConfig jwtConfig = new TokenConfig.JwtConfig();
        jwtConfig.setAlg("RS256");
        jwtConfig.setKeyId("key-1");
        return jwtConfig;
    }

    private JwtKey getJwtKey(final String keyId) {
        final KeyPair keyPair = this.getKeyPair();
        final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        final RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new JwtKey(keyId, publicKey, privateKey);
    }

    private List<JwtKey> getVerificationKeys() {
        return List.of(this.getJwtKey("verify-1"));
    }

    private JwksResponse getJwksResponse() {
        return JwksResponse.builder()
                .keys(List.of())
                .build();
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
