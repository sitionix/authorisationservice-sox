package com.sitionix.athssox.infra;

import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class TestContainerSupport {

    private static final String POSTGRES_SERVICE_NAME = "postgresql";

    private static final Integer POSTGRES_SERVICE_PORT = 5432;

    private static final DockerComposeContainer<?> compose = new DockerComposeContainer<>(
            new File(ClassLoader.getSystemResource("compose/docker-compose.yml").getPath()));

    static {
        final Boolean localContainers = Boolean.parseBoolean(System.getenv("LOCAL_CONTAINERS"));

        if (!localContainers) {
            startCompose();
        }
    }

    public static void startCompose() {
        prepareCompose();

        printLogs();

        compose.waitingFor(POSTGRES_SERVICE_NAME, Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)));

        compose.start();

        configFramework();
    }

    public static void printLogs() {
        final Slf4jLogConsumer consumer = new Slf4jLogConsumer(LoggerFactory.getLogger(TestContainerSupport.class));

        compose.withLogConsumer(POSTGRES_SERVICE_NAME, consumer);
    }

    private static void prepareCompose() {
        final Map<String, Object> composeContent = new Yaml().load(ClassLoader.getSystemResourceAsStream("compose/docker-compose.yml"));
        final Map<String, Object> composeServices = (Map<String, Object>) composeContent.get("services");

        for (final Map.Entry<String, Object> composeServicesEntry : composeServices.entrySet()) {
            final String composeServiceName = composeServicesEntry.getKey();

            final Map<String, Object> composeService = (Map<String, Object>) composeServicesEntry.getValue();

            final List<String> composeServicePorts = (List<String>) composeService.get("ports");

            for (final String composeServicePort : composeServicePorts) {
                compose.withExposedService(composeServiceName, Integer.parseInt(composeServicePort.split(":")[1]),
                        Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofMinutes(5)));
            }
        }
    }

    public static void configFramework() {
        final String postgresHost = compose.getServiceHost(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT);
        final Integer postgresPort = compose.getServicePort(POSTGRES_SERVICE_NAME, POSTGRES_SERVICE_PORT);

        // Використовуємо Spring Boot автоконфігурацію datasource
        System.setProperty("spring.datasource.url", "jdbc:postgresql://" + postgresHost + ":" + postgresPort + "/AUTHS_SOX");
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres-pwd");
    }

}