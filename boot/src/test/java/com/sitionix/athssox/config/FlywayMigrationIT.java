package com.sitionix.athssox.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.file.Path;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIT {

    @Test
    void givenEmptyDatabase_whenFlywayMigratesTwice_thenSchemaIsCreatedAndHistoryIsStable() throws Exception {
        final TimeZone originalTimeZone = TimeZone.getDefault();

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try (final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")) {
            postgres.start();

            final String flywayLocation = "filesystem:" + Path.of("..", "db-migration").toAbsolutePath().normalize();

            final Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations(flywayLocation)
                    .load();

            final var firstResult = flyway.migrate();
            final var secondResult = flyway.migrate();

            assertThat(firstResult.migrationsExecuted).isEqualTo(13);
            assertThat(secondResult.migrationsExecuted).isZero();

            try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement statement = connection.createStatement()) {
                assertThat(this.countRows(statement, "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true"))
                        .isEqualTo(13);
                assertThat(this.countRows(statement, "SELECT COUNT(*) FROM users"))
                        .isZero();
                assertThat(this.countRows(statement, "SELECT COUNT(*) FROM global_roles"))
                        .isEqualTo(4);
                assertThat(this.countRows(statement, "SELECT COUNT(*) FROM user_statuses"))
                        .isEqualTo(4);
                assertThat(this.countRows(statement, "SELECT COUNT(*) FROM email_verification_token_statuses"))
                        .isEqualTo(3);
                assertThat(this.countRows(statement, "SELECT COUNT(*) FROM session_statuses"))
                        .isEqualTo(4);
                assertThat(this.countRows(statement, "SELECT COUNT(*) FROM refresh_token_statuses"))
                        .isEqualTo(2);
                assertThat(this.countRows(statement, "SELECT COUNT(*) FROM forge_outbox_statuses"))
                        .isEqualTo(5);
                assertThat(this.countRows(statement, "SELECT COUNT(*) FROM forge_outbox_aggregate_types"))
                        .isEqualTo(1);
                assertThat(this.countRows(statement, """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_name = 'forge_outbox_events'
                          AND column_name IN (
                              'idempotency_id',
                              'headers',
                              'metadata',
                              'initiator_type',
                              'initiator_id'
                          )
                        """))
                        .isEqualTo(5);
                assertThat(this.countRows(statement, """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_name = 'forge_outbox_events'
                          AND column_name = 'idempotency_id'
                          AND is_nullable = 'NO'
                        """))
                        .isEqualTo(1);
                assertThat(this.countRows(statement, """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_name = 'forge_outbox_events'
                          AND column_name IN ('headers', 'metadata')
                          AND is_nullable = 'NO'
                          AND column_default = '''{}''::jsonb'
                        """))
                        .isEqualTo(2);
                assertThat(this.countRows(statement, """
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_name = 'refresh_tokens'
                          AND column_name = 'status_id'
                          AND is_nullable = 'NO'
                        """))
                        .isEqualTo(1);
            }
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    private long countRows(final Statement statement, final String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
