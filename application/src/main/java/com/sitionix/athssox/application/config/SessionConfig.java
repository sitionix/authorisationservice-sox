package com.sitionix.athssox.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.session")
public class SessionConfig {

    private long lastUsedThrottleMinutes = 5L;

    public Duration getLastUsedThrottleInterval() {
        return Duration.ofMinutes(this.lastUsedThrottleMinutes);
    }
}
