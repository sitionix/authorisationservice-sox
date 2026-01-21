package com.sitionix.athssox.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.cleanup")
public class CleanupConfig {

    private boolean enabled = true;
    private String cron = "0 0 0 * * *";
    private String zone = "Europe/Kiev";
}
