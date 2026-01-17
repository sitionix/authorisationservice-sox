package com.sitionix.athssox.api.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private boolean trustedProxy = false;

    private EndpointLimits login = new EndpointLimits();
    private EndpointLimits register = new EndpointLimits();
    private EndpointLimits refresh = new EndpointLimits();
    private EndpointLimits resend = new EndpointLimits();

    @Getter
    @Setter
    public static class EndpointLimits {
        private Rule ip = new Rule();
        private Rule email = new Rule();
        private Rule session = new Rule();
    }

    @Getter
    @Setter
    public static class Rule {
        private boolean enabled = true;
        private long limit = 0;
        private Duration window = Duration.ZERO;

        public boolean isActive() {
            return this.enabled && this.limit > 0 && this.window != null && !this.window.isZero() && !this.window.isNegative();
        }
    }
}
