package com.sitionix.athssox.config;

import com.sitionix.athssox.domain.model.outbox.NotificationOutboxEventType;
import com.sitionix.forge.outbox.core.model.EnumForgeOutboxEventTypes;
import com.sitionix.forge.outbox.core.model.ForgeOutboxEventTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OutboxEventTypeConfiguration {

    @Bean
    public ForgeOutboxEventTypes forgeOutboxEventTypes() {
        return new EnumForgeOutboxEventTypes<>(NotificationOutboxEventType.class);
    }
}
