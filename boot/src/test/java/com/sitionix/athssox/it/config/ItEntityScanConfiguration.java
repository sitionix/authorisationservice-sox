package com.sitionix.athssox.it.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan(basePackages = {"com.sitionix.athssox.postgresql.entity", "com.sitionix.forge.outbox.postgres.entity"})
public class ItEntityScanConfiguration {
}
