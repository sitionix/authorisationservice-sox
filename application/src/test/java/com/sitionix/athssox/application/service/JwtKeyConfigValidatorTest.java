package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.TokenConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(MockitoExtension.class)
class JwtKeyConfigValidatorTest {

    private JwtKeyConfigValidator jwtKeyConfigValidator;

    @BeforeEach
    void setUp() {
        this.jwtKeyConfigValidator = new JwtKeyConfigValidator();
    }

    @Test
    void givenValidPemConfig_whenValidate_thenNoException() {
        //given
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(this.getPrivateKeyValue(),
                null,
                null,
                null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("RS256",
                this.getKeyId(),
                this.getKeyStoreConfig(null, null, null, null),
                pemConfig,
                List.of());

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.jwtKeyConfigValidator.validate(jwtConfig));

        //then
        assertThat(actualThrowable).isNull();
    }

    @Test
    void givenMissingKeyId_whenValidate_thenThrowException() {
        //given
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(this.getPrivateKeyValue(),
                null,
                null,
                null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("RS256",
                null,
                this.getKeyStoreConfig(null, null, null, null),
                pemConfig,
                List.of());

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.jwtKeyConfigValidator.validate(jwtConfig));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT key id (kid) must be configured.");
    }

    @Test
    void givenInvalidAlg_whenValidate_thenThrowException() {
        //given
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(this.getPrivateKeyValue(),
                null,
                null,
                null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("HS256",
                this.getKeyId(),
                this.getKeyStoreConfig(null, null, null, null),
                pemConfig,
                List.of());

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.jwtKeyConfigValidator.validate(jwtConfig));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only RS256 JWT signing is supported.");
    }

    @Test
    void givenKeyStoreAndPem_whenValidate_thenThrowException() {
        //given
        final TokenConfig.KeyStoreConfig keyStoreConfig = this.getKeyStoreConfig("classpath:keystore.p12",
                "changeit",
                "jwt",
                "changeit");
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(this.getPrivateKeyValue(),
                null,
                null,
                null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("RS256",
                this.getKeyId(),
                keyStoreConfig,
                pemConfig,
                List.of());

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.jwtKeyConfigValidator.validate(jwtConfig));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Configure either a key store or a PEM private key for JWT signing.");
    }

    @Test
    void givenMissingKeyStoreDetails_whenValidate_thenThrowException() {
        //given
        final TokenConfig.KeyStoreConfig keyStoreConfig = this.getKeyStoreConfig("classpath:keystore.p12",
                null,
                "jwt",
                null);
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(null,
                null,
                null,
                null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("RS256",
                this.getKeyId(),
                keyStoreConfig,
                pemConfig,
                List.of());

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.jwtKeyConfigValidator.validate(jwtConfig));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT key store path, alias and password must be configured.");
    }

    @Test
    void givenVerificationKeyMissingMaterial_whenValidate_thenThrowException() {
        //given
        final TokenConfig.VerificationKeyConfig verificationKey = this.getVerificationKeyConfig("old-key",
                null,
                null);
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(this.getPrivateKeyValue(),
                null,
                null,
                null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("RS256",
                this.getKeyId(),
                this.getKeyStoreConfig(null, null, null, null),
                pemConfig,
                List.of(verificationKey));

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.jwtKeyConfigValidator.validate(jwtConfig));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Each verification key must define kid and public key material.");
    }

    private TokenConfig.JwtConfig getJwtConfig(final String alg,
                                               final String keyId,
                                               final TokenConfig.KeyStoreConfig keyStoreConfig,
                                               final TokenConfig.PemConfig pemConfig,
                                               final List<TokenConfig.VerificationKeyConfig> verificationKeys) {
        final TokenConfig.JwtConfig jwtConfig = new TokenConfig.JwtConfig();
        jwtConfig.setAlg(alg);
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

    private String getKeyId() {
        return "key-1";
    }

    private String getPrivateKeyValue() {
        return "private-key";
    }
}
