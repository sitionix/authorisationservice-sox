package com.sitionix.athssox.config.props;

import com.sitionix.athssox.domain.config.OutboxWorkerConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component("outboxWorkerConfig")
@ConfigurationProperties(prefix = "outbox.worker")
public class OutboxWorkerProperties implements OutboxWorkerConfig {

    private int batchSize = 50;
    private long retryDelaySeconds = 60L;
    private int maxRetries = 5;
    private long pollDelayMs = 5000L;
}
