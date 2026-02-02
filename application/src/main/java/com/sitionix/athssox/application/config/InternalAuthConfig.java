package com.sitionix.athssox.application.config;

import com.sitionix.athssox.application.security.internal.InternalAuthMode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.internal-auth")
public class InternalAuthConfig {

    private InternalAuthMode mode;

    private String serviceName;

    private DevJwtConfig dev = new DevJwtConfig();

    private Map<String, PolicyConfig> policies = new HashMap<>();

    @Getter
    @Setter
    public static class DevJwtConfig {

        private String jwtSecret;

        private String issuer = "sitionix-internal";

        private List<String> acceptedAudiences = new ArrayList<>();

        private long ttlSeconds = 300L;
    }

    @Getter
    @Setter
    public static class PolicyConfig {

        private List<String> allow = new ArrayList<>();
    }
}
