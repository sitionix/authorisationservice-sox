package com.sitionix.athssox.it;

import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.postgresql.entity.user.UserEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

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
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(3L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserPendingEmailVerifyEntity.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenValid.json"))
                .build();

        final UserEntity user = this.testManager.postgresql()
                .get(UserEntity.class)
                .hasSize(1)
                .singleElement()
                .assertEntity();
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
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(2)
                .withFetchedRelations()
                .containsAllWithJsons("emailVerificationTokenResendRevokedExpected.json",
                        "emailVerificationTokenResendActiveExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "nextRetryAt", "payload", "createdAt", "updatedAt")
                .containsWithJsonsStrict("outboxEventEmailVerifyEntity.json");
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

        final UserEntity user = this.testManager.postgresql()
                .get(UserEntity.class)
                .hasSize(1)
                .singleElement()
                .assertEntity();
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
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .containsAllWithJsons("emailVerificationTokenActiveExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(0);
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
