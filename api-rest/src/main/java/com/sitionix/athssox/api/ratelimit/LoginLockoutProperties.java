package com.sitionix.athssox.api.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.login-lockout")
public class LoginLockoutProperties {

    private boolean enabled = true;

    private long failureThreshold = 5;

    private Duration failureWindow = Duration.ofMinutes(10);

    private Duration cooldown = Duration.ofMinutes(15);
}
