package com.sitionix.athssox.worker;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "outbox.email-verify")
public class EmailVerifyOutboxWorkerConfig {

    private int batchSize = 50;

    private long retryDelaySeconds = 60L;

    private int maxRetries = 5;
}
