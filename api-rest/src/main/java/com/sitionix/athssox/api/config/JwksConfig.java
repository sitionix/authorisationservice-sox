package com.sitionix.athssox.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.tokens.jwks")
public class JwksConfig {

    private long cacheSeconds = 300L;
}
