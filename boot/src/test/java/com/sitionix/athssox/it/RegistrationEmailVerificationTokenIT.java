package com.sitionix.athssox.it;

import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.sitionix.athssox.application.config.EmailVerificationSecurityConfig;
import com.sitionix.athssox.application.service.HmacEmailVerificationTokenSigner;
import com.sitionix.athssox.domain.service.EmailVerificationTokenIdGenerator;
import com.sitionix.athssox.domain.service.PepperIdGenerator;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Import(RegistrationEmailVerificationTokenIT.TestConfig.class)
class RegistrationEmailVerificationTokenIT {

    private static final UUID FIRST_TOKEN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_TOKEN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID FIRST_PEPPER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SECOND_PEPPER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant FIXED_NOW = Instant.parse("2025-12-23T18:31:16.740787Z");

    @Autowired
    private TestManager testManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DeterministicTokenIdGenerator tokenIdGenerator;

    @Autowired
    private DeterministicPepperIdGenerator pepperIdGenerator;

    @Value("${security.email-verification.hmac-secret}")
    private String hmacSecret;

    @BeforeEach
    void setUp() {
        this.tokenIdGenerator.reset();
        this.pepperIdGenerator.reset();
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
        final String sequenceName = jdbcTemplate.queryForObject("select pg_get_serial_sequence('users', 'id')", String.class);
        jdbcTemplate.execute("alter sequence " + Objects.requireNonNull(sequenceName) + " restart with 1");
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .build();
    }

    @Test
    @DisplayName("Should create outbox event with tokenId and pepperId on registration")
    void givenRegistration_whenRegister_thenOutboxContainsTokenAndPepper() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json")
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "user")
                .containsWithJsonsStrict("emailVerificationTokenRegistrationExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "nextRetryAt", "createdAt", "updatedAt", "aggregateId")
                .containsWithJsonsStrict("outboxEventEmailVerifyEntity.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash")
                .containsWithJsonsStrict("registeredUserEntity.json");
    }

    @Test
    @DisplayName("Should keep pepperId only in outbox payload")
    void givenRegistration_whenRegister_thenPepperNotPersistedInTokenTable() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json")
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "nextRetryAt", "createdAt", "updatedAt", "aggregateId")
                .containsWithJsonsStrict("outboxEventEmailVerifyEntity.json");

        final boolean hasPepperField = Arrays.stream(EmailVerificationTokenEntity.class.getDeclaredFields())
                .map(Field::getName)
                .anyMatch(name -> name.toLowerCase(Locale.ROOT).contains("pepper"));

        assertThat(hasPepperField).isFalse();
    }

    @Test
    @DisplayName("Should generate stable, URL-safe signature for tokenId and pepperId")
    void givenTokenIdAndPepperId_whenSign_thenSignatureIsStableAndUrlSafe() {
        //given
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json")
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        final EmailVerificationSecurityConfig config = new EmailVerificationSecurityConfig();
        config.setHmacSecret(this.hmacSecret);
        final HmacEmailVerificationTokenSigner signer = new HmacEmailVerificationTokenSigner(config);

        //when
        final String first = signer.sign(FIRST_TOKEN_ID, FIRST_PEPPER_ID);
        final String second = signer.sign(FIRST_TOKEN_ID, FIRST_PEPPER_ID);

        //then
        assertThat(first).isEqualTo(second);
        assertThat(first).matches("^[A-Za-z0-9_-]+$");
        assertThat(first).doesNotContain("=");
    }

    @Test
    @DisplayName("Should generate different signatures for different pepperId values")
    void givenSameTokenId_whenSignWithDifferentPepper_thenSignaturesDiffer() {
        //given
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json")
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        final EmailVerificationSecurityConfig config = new EmailVerificationSecurityConfig();
        config.setHmacSecret(this.hmacSecret);
        final HmacEmailVerificationTokenSigner signer = new HmacEmailVerificationTokenSigner(config);

        //when
        final String first = signer.sign(FIRST_TOKEN_ID, FIRST_PEPPER_ID);
        final String second = signer.sign(FIRST_TOKEN_ID, SECOND_PEPPER_ID);

        //then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Should generate unique tokenId and pepperId across registrations")
    void givenTwoRegistrations_whenRegister_thenTokenAndPepperAreUnique() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json", (RegisterUserDTO request) -> request.setEmail("user+first@sitionix.com"))
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json", (RegisterUserDTO request) -> request.setEmail("user+second@sitionix.com"))
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(2)
                .withFetchedRelations()
                .ignoreFields("id", "nextRetryAt", "createdAt", "updatedAt", "aggregateId")
                .containsAllWithJsons("outboxEventEmailVerifyEntityFirst.json", "outboxEventEmailVerifyEntitySecond.json");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        DeterministicTokenIdGenerator tokenIdGenerator() {
            return new DeterministicTokenIdGenerator();
        }

        @Bean
        @Primary
        DeterministicPepperIdGenerator pepperIdGenerator() {
            return new DeterministicPepperIdGenerator();
        }

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }

    static final class DeterministicTokenIdGenerator implements EmailVerificationTokenIdGenerator {

        private final AtomicInteger index = new AtomicInteger();
        private final List<UUID> values = List.of(FIRST_TOKEN_ID, SECOND_TOKEN_ID);

        @Override
        public UUID generate() {
            final int position = this.index.getAndIncrement();
            if (position >= this.values.size()) {
                throw new IllegalStateException("No more token ids configured for integration tests.");
            }
            return this.values.get(position);
        }

        void reset() {
            this.index.set(0);
        }
    }

    static final class DeterministicPepperIdGenerator implements PepperIdGenerator {

        private final AtomicInteger index = new AtomicInteger();
        private final List<UUID> values = List.of(FIRST_PEPPER_ID, SECOND_PEPPER_ID);

        @Override
        public UUID generate() {
            final int position = this.index.getAndIncrement();
            if (position >= this.values.size()) {
                throw new IllegalStateException("No more pepper ids configured for integration tests.");
            }
            return this.values.get(position);
        }

        void reset() {
            this.index.set(0);
        }
    }
}
