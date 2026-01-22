package com.sitionix.athssox.it;

import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.sitionix.athssox.application.config.EmailVerificationSecurityConfig;
import com.sitionix.athssox.application.service.HmacEmailVerificationTokenSigner;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
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

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Import(RegistrationEmailVerificationTokenIT.TestConfig.class)
class RegistrationEmailVerificationTokenIT {

    private static final UUID SITE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_PEPPER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final Instant FIXED_NOW = Instant.parse("2025-12-23T18:31:16.740787Z");
    private static final long EMAIL_VERIFICATION_TTL_SECONDS = 86400L;

    @Autowired
    private TestManager testManager;

    @Value("${security.email-verification.hmac-secret}")
    private String hmacSecret;

    @BeforeEach
    void setUp() {
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
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash")
                .containsWithJsonsStrict("registeredUserEntity.json");

        final List<EmailVerificationTokenEntity> tokens =
                this.testManager.postgresql().get(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT);
        assertThat(tokens).hasSize(1);
        final EmailVerificationTokenEntity token = tokens.get(0);

        assertThat(token.getId()).isNotNull();
        assertThat(token.getTokenHash()).isNotBlank();
        assertThat(token.getSiteId()).isEqualTo(SITE_ID);
        assertThat(token.getUsedAt()).isNull();
        assertThat(token.getExpiresAt()).isEqualTo(FIXED_NOW.plusSeconds(EMAIL_VERIFICATION_TTL_SECONDS));

        final List<OutboxEventEntity> events =
                this.testManager.postgresql().get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        assertThat(events).hasSize(1);
        final String payload = events.get(0).getPayload();
        final String payloadTokenId = JsonPath.read(payload, "$.params.emailVerificationTokenId");
        final String payloadPepperId = JsonPath.read(payload, "$.params.pepperId");

        assertThat(payloadTokenId).isEqualTo(token.getId().toString());
        assertThat(payloadPepperId).isNotBlank();
        assertThat(UUID.fromString(payloadPepperId)).isNotNull();
        assertThat(payload).doesNotContain("verifyUrl");
        assertThat(payload).doesNotContain("token=");
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
        final List<OutboxEventEntity> events =
                this.testManager.postgresql().get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        assertThat(events).hasSize(1);
        final String payload = events.get(0).getPayload();
        final String payloadPepperId = JsonPath.read(payload, "$.params.pepperId");

        assertThat(payloadPepperId).isNotBlank();
        assertThat(UUID.fromString(payloadPepperId)).isNotNull();

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

        final List<OutboxEventEntity> events =
                this.testManager.postgresql().get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        assertThat(events).hasSize(1);
        final String payload = events.get(0).getPayload();
        final UUID tokenId = UUID.fromString(JsonPath.read(payload, "$.params.emailVerificationTokenId"));
        final UUID pepperId = UUID.fromString(JsonPath.read(payload, "$.params.pepperId"));

        final EmailVerificationSecurityConfig config = new EmailVerificationSecurityConfig();
        config.setHmacSecret(this.hmacSecret);
        final HmacEmailVerificationTokenSigner signer = new HmacEmailVerificationTokenSigner(config);

        //when
        final String first = signer.sign(tokenId, pepperId);
        final String second = signer.sign(tokenId, pepperId);

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

        final List<OutboxEventEntity> events =
                this.testManager.postgresql().get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        assertThat(events).hasSize(1);
        final String payload = events.get(0).getPayload();
        final UUID tokenId = UUID.fromString(JsonPath.read(payload, "$.params.emailVerificationTokenId"));
        final UUID pepperId = UUID.fromString(JsonPath.read(payload, "$.params.pepperId"));

        final EmailVerificationSecurityConfig config = new EmailVerificationSecurityConfig();
        config.setHmacSecret(this.hmacSecret);
        final HmacEmailVerificationTokenSigner signer = new HmacEmailVerificationTokenSigner(config);

        //when
        final String first = signer.sign(tokenId, pepperId);
        final String second = signer.sign(tokenId, OTHER_PEPPER_ID);

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
        final List<OutboxEventEntity> events =
                this.testManager.postgresql().get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        assertThat(events).hasSize(2);
        final String firstPayload = events.get(0).getPayload();
        final String secondPayload = events.get(1).getPayload();
        final String firstTokenId = JsonPath.read(firstPayload, "$.params.emailVerificationTokenId");
        final String secondTokenId = JsonPath.read(secondPayload, "$.params.emailVerificationTokenId");
        final String firstPepperId = JsonPath.read(firstPayload, "$.params.pepperId");
        final String secondPepperId = JsonPath.read(secondPayload, "$.params.pepperId");

        assertThat(firstTokenId).isNotBlank();
        assertThat(secondTokenId).isNotBlank();
        assertThat(firstPepperId).isNotBlank();
        assertThat(secondPepperId).isNotBlank();
        assertThat(firstTokenId).isNotEqualTo(secondTokenId);
        assertThat(firstPepperId).isNotEqualTo(secondPepperId);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        }
    }
}
