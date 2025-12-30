package com.sitionix.athssox.domain.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OutboxWorkerConfig {

    private final int batchSize;
    private final long retryDelaySeconds;
    private final int maxRetries;
    private final long pollDelayMs;
}
