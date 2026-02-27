package com.sitionix.athssox.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.cleanup")
public class CleanupConfig {

    private boolean enabled = true;
    private String cron = "0 0 0 * * *";
    private String zone = "Europe/Kiev";
    private Duration refreshTokenRetention = Duration.ofDays(14);
    private Duration emailVerificationTokenRetention = Duration.ofDays(2);
}
