package com.sitionix.athssox.domain.config;

public interface OutboxWorkerConfig {

    int getBatchSize();

    long getRetryDelaySeconds();

    int getMaxRetries();

    long getPollDelayMs();
}
