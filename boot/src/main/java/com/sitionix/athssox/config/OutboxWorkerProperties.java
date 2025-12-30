package com.sitionix.athssox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "outbox.worker")
public class OutboxWorkerProperties {

    private int batchSize = 50;
    private long retryDelaySeconds = 60L;
    private int maxRetries = 5;
    private long pollDelayMs = 5000L;
}
