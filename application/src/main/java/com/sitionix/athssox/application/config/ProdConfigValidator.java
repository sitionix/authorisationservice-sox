package com.sitionix.athssox.application.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Profile("prod")
@Component
public class ProdConfigValidator {

    private final Environment environment;

    public ProdConfigValidator(final Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        final String ddl = this.resolveHibernateDdl();
        if (StringUtils.hasText(ddl)) {
            final String normalized = ddl.trim().toLowerCase();
            if ("update".equals(normalized) || "create".equals(normalized) || "create-drop".equals(normalized)) {
                throw new IllegalStateException("DDL auto update/create is not allowed in prod.");
            }
        }

        final String baseUrl = this.environment.getProperty("bff.base-url");
        if (!StringUtils.hasText(baseUrl) || !baseUrl.startsWith("https://")) {
            throw new IllegalStateException("bff.base-url must be configured with https in prod.");
        }
    }

    private String resolveHibernateDdl() {
        final String ddl = this.environment.getProperty("spring.jpa.hibernate.ddl");
        if (StringUtils.hasText(ddl)) {
            return ddl;
        }
        return this.environment.getProperty("spring.jpa.hibernate.ddl-auto");
    }
}
