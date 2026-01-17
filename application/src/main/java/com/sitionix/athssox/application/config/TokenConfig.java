package com.sitionix.athssox.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.tokens")
public class TokenConfig {

    private String issuer = "athssox";

    private long accessTokenTtlSeconds = 3600L;

    private long refreshTokenTtlSeconds = 2592000L;

    private long emailVerificationTokenTtlSeconds = 86400L;

    private JwtConfig jwt = new JwtConfig();

    @Getter
    @Setter
    public static class JwtConfig {

        private String alg = "RS256";

        private String keyId;

        private KeyStoreConfig keyStore = new KeyStoreConfig();

        private PemConfig pem = new PemConfig();

        private List<VerificationKeyConfig> verificationKeys = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class KeyStoreConfig {

        private String path;

        private String password;

        private String alias;

        private String keyPassword;
    }

    @Getter
    @Setter
    public static class PemConfig {

        private String privateKeyPath;

        private String publicKeyPath;

        private String privateKey;

        private String publicKey;
    }

    @Getter
    @Setter
    public static class VerificationKeyConfig {

        private String keyId;

        private String publicKeyPath;

        private String publicKey;
    }
}
