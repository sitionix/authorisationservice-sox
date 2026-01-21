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
    private Retention refreshTokens = new Retention(14);
    private Retention emailVerificationTokens = new Retention(2);
    private Retention outboxEvents = new Retention(14);
    private Retention deviceSessions = new Retention(0);

    @Getter
    @Setter
    public static class Retention {
        private long retentionDays;

        public Retention(final long retentionDays) {
            this.retentionDays = retentionDays;
        }
    }
}
