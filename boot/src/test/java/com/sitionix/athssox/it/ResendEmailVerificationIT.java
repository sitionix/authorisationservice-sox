package com.sitionix.athssox.it;

import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenEntity;
import com.sitionix.athssox.postgresql.entity.user.UserEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ResendEmailVerificationIT {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should resend email verification for pending user and create outbox event")
    void givenPendingUser_whenResendEmailVerification_thenTokenReissuedAndOutboxCreated() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(3L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserPendingEmailVerifyEntity.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenValid.json"))
                .build();

        final List<UserEntity> users = this.testManager.postgresql()
                .get(DatabaseContract.USER_ENTITY_DB_CONTRACT);
        assertThat(users).hasSize(1);
        final UserEntity user = users.get(0);
        final Long userId = user.getId();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.resendEmailVerification())
                .withRequest("resendEmailVerificationRequest.json")
                .header("X-Forge-User-Sub", userId.toString())
                .expectResponse("resendEmailVerificationResponse.json")
                .expectStatus(HttpStatus.ACCEPTED)
                .assertAndCreate();

        //then
        final List<EmailVerificationTokenEntity> tokens = this.testManager.postgresql()
                .get(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT);
        assertThat(tokens).hasSize(2);

        final UUID previousTokenId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        final EmailVerificationTokenEntity revokedToken = tokens.stream()
                .filter(token -> Objects.equals(token.getId(), previousTokenId))
                .findFirst()
                .orElseThrow();
        assertThat(revokedToken.getStatus().getId()).isEqualTo(3L);

        final List<EmailVerificationTokenEntity> activeTokens = tokens.stream()
                .filter(token -> Objects.equals(token.getStatus().getId(), 1L))
                .toList();
        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.get(0).getId()).isNotEqualTo(previousTokenId);

        final List<OutboxEventEntity> outboxEvents = this.testManager.postgresql()
                .get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        assertThat(outboxEvents).hasSize(1);
        final OutboxEventEntity outboxEvent = outboxEvents.get(0);
        assertThat(outboxEvent.getAggregateType().getId()).isEqualTo(1L);
        assertThat(outboxEvent.getAggregateId()).isEqualTo(userId);
        assertThat(outboxEvent.getEventType().getId()).isEqualTo(1L);
        assertThat(outboxEvent.getStatus().getId()).isEqualTo(1L);
        assertThat(outboxEvent.getRetryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return accepted and do nothing for active user")
    void givenActiveUser_whenResendEmailVerification_thenNoOutboxAndNoNewToken() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActiveStatusEntity.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenValid.json"))
                .build();

        final List<UserEntity> users = this.testManager.postgresql()
                .get(DatabaseContract.USER_ENTITY_DB_CONTRACT);
        assertThat(users).hasSize(1);
        final UserEntity user = users.get(0);
        final Long userId = user.getId();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.resendEmailVerification())
                .withRequest("resendEmailVerificationRequest.json")
                .header("X-Forge-User-Sub", userId.toString())
                .expectResponse("resendEmailVerificationResponse.json")
                .expectStatus(HttpStatus.ACCEPTED)
                .assertAndCreate();

        //then
        final List<EmailVerificationTokenEntity> tokens = this.testManager.postgresql()
                .get(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT);
        assertThat(tokens).hasSize(1);
        final EmailVerificationTokenEntity token = tokens.get(0);
        assertThat(token.getId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(token.getStatus().getId()).isEqualTo(1L);

        final List<OutboxEventEntity> outboxEvents = this.testManager.postgresql()
                .get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        assertThat(outboxEvents).isEmpty();
    }

    @Test
    @DisplayName("Should return 401 when authorization header is missing")
    void givenMissingAuthorization_whenResendEmailVerification_thenUnauthorized() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.resendEmailVerification())
                .withRequest("resendEmailVerificationRequest.json")
                .token("")
                .expectStatus(HttpStatus.UNAUTHORIZED)
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.code").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.title").value(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should return 401 when authorization token is invalid")
    void givenInvalidAuthorization_whenResendEmailVerification_thenUnauthorized() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.resendEmailVerification())
                .withRequest("resendEmailVerificationRequest.json")
                .token("Bearer invalid")
                .expectStatus(HttpStatus.UNAUTHORIZED)
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.code").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.title").value(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
                .assertAndCreate();

        //then
    }
}
