package com.sitionix.athssox.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.email-verification.resend")
public class EmailVerificationResendConfig {

    private long cooldownSeconds = 60L;

    private long dailyCap = 5L;
}
