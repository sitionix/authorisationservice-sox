package com.sitionix.athssox.application.config;

import com.sitionix.athssox.application.outbox.storage.AuthOutboxStorageAdapter;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import com.sitionix.forge.outbox.core.port.OutboxStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ForgeOutboxBridgeConfig {

    @Bean
    public OutboxStorage authOutboxStorage(final OutboxEventRepository outboxEventRepository) {
        return new AuthOutboxStorageAdapter(outboxEventRepository);
    }
}
