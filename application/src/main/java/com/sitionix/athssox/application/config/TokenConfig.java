package com.sitionix.athssox.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.tokens")
public class TokenConfig {

    private String issuer = "athssox";

    private String jwtSecret = "change-me";

    private long accessTokenTtlSeconds = 3600L;

    private long refreshTokenTtlSeconds = 2592000L;
}
