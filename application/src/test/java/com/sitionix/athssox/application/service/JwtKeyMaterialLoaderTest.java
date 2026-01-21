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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
    void given_pem_config_with_private_and_public_when_load_active_key_then_return_key_pair() {
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
    void given_pem_config_with_private_only_when_load_active_key_then_derive_public_key() {
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
    void given_inline_verification_keys_when_load_verification_keys_then_return_keys() {
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
    void given_verification_key_path_when_load_verification_keys_then_load_from_resource() throws Exception {
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

    @Test
    void given_rsa_private_key_pem_when_load_active_key_then_throw_exception() {
        //given
        final String privatePem = this.getRsaPrivateKeyPem();
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(privatePem, null, null, null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("key-5",
                this.getKeyStoreConfig(null, null, null, null),
                pemConfig,
                List.of());

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.jwtKeyMaterialLoader.loadActiveKey(jwtConfig));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("RSA PRIVATE KEY format is not supported; use PKCS#8 private key.");
    }

    @Test
    void given_missing_private_key_resource_when_load_active_key_then_throw_exception() {
        //given
        final String privateKeyPath = this.getMissingResourcePath();
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(null, privateKeyPath, null, null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("key-6",
                this.getKeyStoreConfig(null, null, null, null),
                pemConfig,
                List.of());

        when(this.resourceLoader.getResource(privateKeyPath))
                .thenReturn(this.resource);
        when(this.resource.exists())
                .thenReturn(false);

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.jwtKeyMaterialLoader.loadActiveKey(jwtConfig));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT key resource not found: " + privateKeyPath);
        verify(this.resourceLoader)
                .getResource(privateKeyPath);
        verify(this.resource)
                .exists();
    }

    @Test
    void given_key_store_config_when_load_active_key_then_return_key_pair() throws Exception {
        //given
        final String keyStorePath = this.getKeyStorePath();
        final String keyStorePassword = this.getKeyStorePassword();
        final String keyStoreKeyPassword = this.getKeyStoreKeyPassword();
        final String keyStoreAlias = this.getKeyStoreAlias();
        final InputStream keyStoreInputStream = this.getKeyStoreInputStream(keyStoreAlias,
                keyStorePassword,
                keyStoreKeyPassword);
        final TokenConfig.KeyStoreConfig keyStoreConfig = this.getKeyStoreConfig(keyStorePath,
                keyStorePassword,
                keyStoreAlias,
                keyStoreKeyPassword);
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(null, null, null, null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("key-8",
                keyStoreConfig,
                pemConfig,
                List.of());

        when(this.resourceLoader.getResource(keyStorePath))
                .thenReturn(this.resource);
        when(this.resource.exists())
                .thenReturn(true);
        when(this.resource.getInputStream())
                .thenReturn(keyStoreInputStream);

        //when
        final JwtKey actual = this.jwtKeyMaterialLoader.loadActiveKey(jwtConfig);

        //then
        final RSAPrivateKey expectedPrivateKey = this.getKeyStorePrivateKey();
        final RSAPublicKey expectedPublicKey = this.getKeyStorePublicKey();

        assertThat(actual.getKeyId()).isEqualTo("key-8");
        assertThat(actual.getPublicKey().getModulus()).isEqualTo(expectedPublicKey.getModulus());
        assertThat(actual.getPublicKey().getPublicExponent()).isEqualTo(expectedPublicKey.getPublicExponent());
        assertThat(actual.getPrivateKey().getModulus()).isEqualTo(expectedPrivateKey.getModulus());
        assertThat(actual.getPrivateKey().getPrivateExponent()).isEqualTo(expectedPrivateKey.getPrivateExponent());
        verify(this.resourceLoader)
                .getResource(keyStorePath);
        verify(this.resource)
                .exists();
        verify(this.resource)
                .getInputStream();
    }

    @Test
    void given_missing_alias_in_key_store_when_load_active_key_then_throw_exception() throws Exception {
        //given
        final String keyStorePath = this.getKeyStorePath();
        final String keyStorePassword = this.getKeyStorePassword();
        final String keyStoreKeyPassword = this.getKeyStoreKeyPassword();
        final InputStream keyStoreInputStream = this.getKeyStoreInputStream(this.getKeyStoreAlias(),
                keyStorePassword,
                keyStoreKeyPassword);
        final TokenConfig.KeyStoreConfig keyStoreConfig = this.getKeyStoreConfig(keyStorePath,
                keyStorePassword,
                "missing-alias",
                keyStoreKeyPassword);
        final TokenConfig.PemConfig pemConfig = this.getPemConfig(null, null, null, null);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("key-9",
                keyStoreConfig,
                pemConfig,
                List.of());

        when(this.resourceLoader.getResource(keyStorePath))
                .thenReturn(this.resource);
        when(this.resource.exists())
                .thenReturn(true);
        when(this.resource.getInputStream())
                .thenReturn(keyStoreInputStream);

        //when
        final Throwable actualThrowable = catchThrowable(() -> this.jwtKeyMaterialLoader.loadActiveKey(jwtConfig));

        //then
        assertThat(actualThrowable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to load JWT keys from key store.")
                .hasRootCauseMessage("JWT key alias not found in key store.");
        verify(this.resourceLoader)
                .getResource(keyStorePath);
        verify(this.resource)
                .exists();
        verify(this.resource)
                .getInputStream();
    }

    @Test
    void given_null_verification_keys_when_load_verification_keys_then_return_empty_list() {
        //given
        final KeyPair signingKeyPair = this.getKeyPair();
        final String signingPrivatePem = this.getPrivateKeyPem(signingKeyPair);
        final TokenConfig.JwtConfig jwtConfig = this.getJwtConfig("key-7",
                this.getKeyStoreConfig(null, null, null, null),
                this.getPemConfig(signingPrivatePem, null, null, null),
                null);

        //when
        final List<JwtKey> actual = this.jwtKeyMaterialLoader.loadVerificationKeys(jwtConfig);

        //then
        assertThat(actual).isEmpty();
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

    private String getMissingResourcePath() {
        return "classpath:missing-private-key.pem";
    }

    private String getRsaPrivateKeyPem() {
        return "-----BEGIN RSA PRIVATE KEY-----\nabc\n-----END RSA PRIVATE KEY-----";
    }

    private String getKeyStorePath() {
        return "classpath:keystore.p12";
    }

    private String getKeyStorePassword() {
        return "changeit";
    }

    private String getKeyStoreKeyPassword() {
        return "changeit";
    }

    private String getKeyStoreAlias() {
        return "jwt";
    }

    private RSAPrivateKey getKeyStorePrivateKey() {
        return this.getPrivateKeyFromPem(this.getKeyStorePrivateKeyPem());
    }

    private RSAPublicKey getKeyStorePublicKey() {
        return (RSAPublicKey) this.getCertificateFromPem(this.getKeyStoreCertificatePem()).getPublicKey();
    }

    private InputStream getKeyStoreInputStream(final String alias,
                                               final String storePassword,
                                               final String keyPassword) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, storePassword.toCharArray());
        keyStore.setKeyEntry(alias,
                this.getPrivateKeyFromPem(this.getKeyStorePrivateKeyPem()),
                keyPassword.toCharArray(),
                new Certificate[]{this.getCertificateFromPem(this.getKeyStoreCertificatePem())});
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        keyStore.store(outputStream, storePassword.toCharArray());
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private RSAPrivateKey getPrivateKeyFromPem(final String pem) {
        try {
            final String normalized = this.normalizePem(pem, "PRIVATE KEY");
            final byte[] decoded = Base64.getDecoder().decode(normalized);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            final PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            return (RSAPrivateKey) privateKey;
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed to parse test private key.", ex);
        }
    }

    private Certificate getCertificateFromPem(final String pem) {
        try {
            final String normalized = this.normalizePem(pem, "CERTIFICATE");
            final byte[] decoded = Base64.getDecoder().decode(normalized);
            final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return certificateFactory.generateCertificate(new ByteArrayInputStream(decoded));
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed to parse test certificate.", ex);
        }
    }

    private String normalizePem(final String pem, final String type) {
        return pem.replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
    }

    private String getKeyStorePrivateKeyPem() {
        return "-----BEGIN PRIVATE KEY-----\n"
                + "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDOqfkbmzTLdBe9\n"
                + "D3M819nXwAL8sNwye0AFmoEsfZaH2OmekjcRP/GxphDH8JwKvOg3pOB+W7MI0XxO\n"
                + "o23Wz8E/znkiz7jo7gzU9owr+sitVFgVRKfTWNJQm4Gf1EseU8mEk0WMAc+WScUu\n"
                + "f+p3JIWETJZSP27X9ByOxgJM7jHnvpfQXNIsd0MdHZw8+NK7xDlnXIx6U8DIO8u6\n"
                + "RcJpv6GNaT79eI21fTK0kqh6Ry2omHYzIMLlbkpFY/RICvuCc+It3qTjkn7u6CYF\n"
                + "V0hxKODfexkv78Me+eih+GGwo2dK1WtkGegZZqjnj55sXrAZpjeeAbLpMI1Sfwpc\n"
                + "9IOKCQh1AgMBAAECggEATsX6NnwcDRr0u3NKrRvnpLXDTbdKT5GsLxab+y3ptWFI\n"
                + "d1FrXQ9fHKRcjCGaEPu9lgwqJ+/jF87lz4G66eDp29zjTpIX/O1UBc7uUgs2996I\n"
                + "7p5rTAN0sxX7PEVq1qYAT6DlJv8/tZzr5JQMdpp8rBeXSsV0Biu9wOwXz2QRsK/E\n"
                + "+AFJUje4rSLeyRrsDknKGbomaLVOmNUXQ+a9CY7UfPpxfGKzi7e5phWA9vzEW3Dh\n"
                + "28JO6Y7VlEkC6xATOyThx9R8sbBsxov91RUln0vmUBlnvENPV7bEU6kA1iY7D33D\n"
                + "NKNKawyEWH0N548DI3qFe/zTUYo3ko5dF2KJVN8StwKBgQD5dKOnNhhN6c1NjWMI\n"
                + "2gLtOoVS00dokHo71r1b3+6hv9eMHcO+JalhiReXT287mQHVSHDl5P/POBU8mY6C\n"
                + "UXsD7oQ5kRAZSDff1QbdLwGzMm/bd8tWpIzlz+zx4jjkrW1zrSy3eKztYXfyoq1Z\n"
                + "hf+rNcyzq49hXzNs0TZ67f0aMwKBgQDUFfEuFTY6bqYrxW6EQoyIItDMXo/b7b+Q\n"
                + "DYdwF3CdywyANFPCA8p6NmNeOiwSqvnsZuqjjtR83/l87IhjiPhy4de3UIKBSk4n\n"
                + "75aInUcpSjHU5A9BY+o8Z5sKupHz3PibFX67O8x/Z2VHuvzah681WarE2qth9anO\n"
                + "7vuKNOV6twKBgFcCSw7IvIN4mGaDeVaeDEVIkzZHgXHEaw4yIYxJOEfszTcPLrZF\n"
                + "FqZxlevuBgNIpC4LfW2CiYNysV2Yxe9IklrVuAMISRA4c1y8Rg+iFLq3TfLD/wWy\n"
                + "kX6bHq9j06pXn4Sok59PFSobRCY6fbJrHblPI55LqdAxz5MWCiWomw6nAoGBAMrm\n"
                + "zx2CZ6YRc+LOeMQKEv6vKElNcp4635lGiDuiATjFKImDBlrIW67+V6SvTLjzYDVd\n"
                + "5L9jh0CM2tg8RuPLB76I+GLFoIVm/EzSt5atuYX0GKaPJVUu7MqraDYkdjvzPNjK\n"
                + "ALvKIwQEfG3ao8vCIcqNpYW9mlcWu0Vhd78qih0TAoGATfI6KcF+wj12HW1l+0ff\n"
                + "1lmj58W9rR3hZveVXyys1qdqnZ20eb7fuZVqHW0AuZmyOk9KHsFd9XI6oPDvLSXt\n"
                + "g05BH3Ik4V0CkAYsZ94I6xnZPJxkp4plAvAo+M3I7Hzj76awU+ofGgbbdDHFgKwS\n"
                + "cjePhU1pL/aHPVPOtjN+SW8=\n"
                + "-----END PRIVATE KEY-----";
    }

    private String getKeyStoreCertificatePem() {
        return "-----BEGIN CERTIFICATE-----\n"
                + "MIIDBzCCAe+gAwIBAgIUaPTctInT5cB9k/1wUjwrOKx0haIwDQYJKoZIhvcNAQEL\n"
                + "BQAwEzERMA8GA1UEAwwIand0LXRlc3QwHhcNMjYwMTE3MTA1NjUyWhcNMzYwMTE1\n"
                + "MTA1NjUyWjATMREwDwYDVQQDDAhqd3QtdGVzdDCCASIwDQYJKoZIhvcNAQEBBQAD\n"
                + "ggEPADCCAQoCggEBAM6p+RubNMt0F70PczzX2dfAAvyw3DJ7QAWagSx9lofY6Z6S\n"
                + "NxE/8bGmEMfwnAq86Dek4H5bswjRfE6jbdbPwT/OeSLPuOjuDNT2jCv6yK1UWBVE\n"
                + "p9NY0lCbgZ/USx5TyYSTRYwBz5ZJxS5/6nckhYRMllI/btf0HI7GAkzuMee+l9Bc\n"
                + "0ix3Qx0dnDz40rvEOWdcjHpTwMg7y7pFwmm/oY1pPv14jbV9MrSSqHpHLaiYdjMg\n"
                + "wuVuSkVj9EgK+4Jz4i3epOOSfu7oJgVXSHEo4N97GS/vwx756KH4YbCjZ0rVa2QZ\n"
                + "6BlmqOePnmxesBmmN54BsukwjVJ/Clz0g4oJCHUCAwEAAaNTMFEwHQYDVR0OBBYE\n"
                + "FMU7mnToN4k8lFUb3r2B5yvt3YMJMB8GA1UdIwQYMBaAFMU7mnToN4k8lFUb3r2B\n"
                + "5yvt3YMJMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBADOm0VcG\n"
                + "ctvBcibozyVtADwb4ZEKiXRJ4tmTk0ykriFokF5RBzNDyLwpM1Y1BvEgphkgGcVT\n"
                + "X2Arc6c5ZIF+LrVcK03mvv9o9fofB+2IKNiYfV8C6dhFNng4jsxTqPoVNqS93EH0\n"
                + "F1aFMTYMmrT6pUdDBcceY6BVS5rMH5SLDfMhYNO54O2vup6iZSfLzO2ehO+Mszxr\n"
                + "OarTgBKH+rnuuibk2ZHcYkNrQbONZcHK8LOnwneXfhdxz7MN9qNFevfFbAQ4LzxB\n"
                + "IGhmMF/AFW7xXDdKOwk8Ij6YQPIGxNeHfUjcWWGzMPbGsBMJRb+XuuCIu0xUHEYi\n"
                + "RROv817Uueyrqkg=\n"
                + "-----END CERTIFICATE-----";
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
