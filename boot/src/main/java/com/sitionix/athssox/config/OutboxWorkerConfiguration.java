package com.sitionix.athssox.config;

import com.sitionix.athssox.domain.config.OutboxWorkerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OutboxWorkerProperties.class)
public class OutboxWorkerConfiguration {

    @Bean
    public OutboxWorkerConfig outboxWorkerConfig(final OutboxWorkerProperties properties) {
        return new OutboxWorkerConfig(
                properties.getBatchSize(),
                properties.getRetryDelaySeconds(),
                properties.getMaxRetries(),
                properties.getPollDelayMs());
    }
}
