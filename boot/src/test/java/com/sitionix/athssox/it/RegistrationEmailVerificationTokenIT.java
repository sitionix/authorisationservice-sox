package com.sitionix.athssox.it;

import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.jayway.jsonpath.JsonPath;
import com.sitionix.athssox.application.config.EmailVerificationSecurityConfig;
import com.sitionix.athssox.application.service.HmacEmailVerificationTokenSigner;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class RegistrationEmailVerificationTokenIT {

    @Autowired
    private TestManager testManager;

    @Value("${security.email-verification.hmac-secret}")
    private String hmacSecret;

    @Test
    @DisplayName("Should create outbox event with tokenId and pepperId on registration")
    void given_registration_when_register_then_outbox_contains_token_and_pepper() {
        //given
        this.seedRegistrationDependencies();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json")
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        final List<EmailVerificationTokenEntity> tokens =
                this.testManager.postgresql().get(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT);
        final List<OutboxEventEntity> events =
                this.testManager.postgresql().get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);

        assertThat(tokens).hasSize(1);
        assertThat(events).hasSize(1);

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "nextRetryAt", "payload", "createdAt", "updatedAt", "aggregateId")
                .containsWithJsonsStrict("outboxEventEmailVerifyEntity.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash")
                .containsWithJsonsStrict("registeredUserEntity.json");

        final UUID tokenId = tokens.get(0).getId();
        final String payload = events.get(0).getPayload();
        final UUID payloadTokenId = this.getUuid(payload, "$.params.emailVerificationTokenId");
        final UUID pepperId = this.getUuid(payload, "$.params.pepperId");

        assertThat(payloadTokenId).isEqualTo(tokenId);
        assertThat(pepperId).isNotNull();
        assertThat(payload).doesNotContain("verifyUrl");
        assertThat(payload).doesNotContain("token=");
    }

    @Test
    @DisplayName("Should keep pepperId only in outbox payload")
    void given_registration_when_register_then_pepper_not_persisted_in_token_table() {
        //given
        this.seedRegistrationDependencies();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json", (RegisterUserDTO request) -> request.setEmail(this.getEmail("pepper-only")))
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        final List<OutboxEventEntity> events =
                this.testManager.postgresql().get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        final String payload = events.get(0).getPayload();
        final UUID pepperId = this.getUuid(payload, "$.params.pepperId");

        assertThat(pepperId).isNotNull();
        assertThat(this.hasPepperField(EmailVerificationTokenEntity.class)).isFalse();
    }

    @Test
    @DisplayName("Should generate stable, URL-safe signature for tokenId and pepperId")
    void given_token_id_and_pepper_id_when_sign_then_signature_is_stable_and_url_safe() {
        //given
        this.seedRegistrationDependencies();
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json", (RegisterUserDTO request) -> request.setEmail(this.getEmail("signature-stable")))
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        final List<OutboxEventEntity> events =
                this.testManager.postgresql().get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        final String payload = events.get(0).getPayload();
        final UUID tokenId = this.getUuid(payload, "$.params.emailVerificationTokenId");
        final UUID pepperId = this.getUuid(payload, "$.params.pepperId");

        //when
        final String first = this.sign(tokenId, pepperId);
        final String second = this.sign(tokenId, pepperId);

        //then
        assertThat(first).isEqualTo(second);
        assertThat(first).matches("^[A-Za-z0-9_-]+$");
        assertThat(first).doesNotContain("=");
    }

    @Test
    @DisplayName("Should generate different signatures for different pepperId values")
    void given_same_token_id_when_sign_with_different_pepper_then_signatures_differ() {
        //given
        this.seedRegistrationDependencies();
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json", (RegisterUserDTO request) -> request.setEmail(this.getEmail("signature-diff")))
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        final List<OutboxEventEntity> events =
                this.testManager.postgresql().get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        final String payload = events.get(0).getPayload();
        final UUID tokenId = this.getUuid(payload, "$.params.emailVerificationTokenId");
        final UUID pepperId = this.getUuid(payload, "$.params.pepperId");
        final UUID otherPepperId = this.getOtherPepperId();

        //when
        final String first = this.sign(tokenId, pepperId);
        final String second = this.sign(tokenId, otherPepperId);

        //then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Should generate unique tokenId and pepperId across registrations")
    void given_two_registrations_when_register_then_token_and_pepper_are_unique() {
        //given
        this.seedRegistrationDependencies();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json", (RegisterUserDTO request) -> request.setEmail(this.getEmail("first")))
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json", (RegisterUserDTO request) -> request.setEmail(this.getEmail("second")))
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        final List<OutboxEventEntity> events =
                this.testManager.postgresql().get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        assertThat(events).hasSize(2);

        final UUID firstTokenId = this.getUuid(events.get(0).getPayload(), "$.params.emailVerificationTokenId");
        final UUID firstPepperId = this.getUuid(events.get(0).getPayload(), "$.params.pepperId");
        final UUID secondTokenId = this.getUuid(events.get(1).getPayload(), "$.params.emailVerificationTokenId");
        final UUID secondPepperId = this.getUuid(events.get(1).getPayload(), "$.params.pepperId");

        assertThat(firstTokenId).isNotEqualTo(secondTokenId);
        assertThat(firstPepperId).isNotEqualTo(secondPepperId);
    }

    private void seedRegistrationDependencies() {
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .build();
    }

    private UUID getUuid(final String payload, final String jsonPath) {
        final String value = JsonPath.read(payload, jsonPath);
        return UUID.fromString(value);
    }

    private UUID getOtherPepperId() {
        return UUID.fromString("f5f9f7e5-dadc-4dcb-8b8c-5d6ab8f7c0b8");
    }

    private String getEmail(final String suffix) {
        return "user+" + suffix + "@sitionix.com";
    }

    private boolean hasPepperField(final Class<?> type) {
        for (final Field field : type.getDeclaredFields()) {
            if (field.getName().toLowerCase().contains("pepper")) {
                return true;
            }
        }
        return false;
    }

    private String sign(final UUID tokenId, final UUID pepperId) {
        final EmailVerificationSecurityConfig config = new EmailVerificationSecurityConfig();
        config.setHmacSecret(this.hmacSecret);
        return new HmacEmailVerificationTokenSigner(config).sign(tokenId, pepperId);
    }
}
