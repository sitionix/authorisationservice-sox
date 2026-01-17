package com.sitionix.athssox.it;

import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.postgresql.entity.user.UserEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class UserControllerIT {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should register new user successfully without siteId for global role")
    void givenGlobalRoleWithoutSiteId_whenRegisterUser_thenSuccessAndUserPersisted() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest_globalRoleNoSiteId.json")
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "id", "passwordHash", "updatedAt")
                .containsWithJsonsStrict("registeredUserGlobalEntity.json");
    }

    @Test
    @DisplayName("Should return 400 without siteId for SITE_ADMIN role")
    void givenSiteAdminRoleWithoutSiteId_whenRegisterUser_thenReturnBadRequest() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest_siteAdminRoleNoSiteId.json")
                .expectStatus(HttpStatus.BAD_REQUEST)
                .expectResponse("responseBadRequest.json")
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should register a new user successfully and persist it")
    void givenValidUserData_whenRegisterUser_thenSuccessAndUserPersisted() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json")
                .expectResponse("registerUserResponse.json", "userId")
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "id", "passwordHash", "updatedAt")
                .containsWithJsonsStrict("registeredUserEntity.json");

        final List<UserEntity> persistedUsers =
                this.testManager.postgresql().get(DatabaseContract.USER_ENTITY_DB_CONTRACT);
        assertThat(persistedUsers).hasSize(1);
        final UserEntity persistedUser = persistedUsers.get(0);

        assertThat(persistedUser.getCreatedAt()).isNotNull();
        assertThat(persistedUser.getPasswordHash()).isNotBlank();
        assertThat(persistedUser.getPasswordHash()).isNotEqualTo(this.getDefaultPassword());

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "nextRetryAt", "payload", "createdAt", "updatedAt", "aggregateId")
                .containsWithJsonsStrict("outboxEventEmailVerifyEntity.json");
    }

    static Stream<Arguments> invalidRegisterUserRequests() {
        return Stream.of(
                Arguments.of(
                        "body is empty",
                        "registerUserRequest_empty.json",
                        (Consumer<RegisterUserDTO>) request -> {
                        }
                ),
                Arguments.of(
                        "email is invalid",
                        "registerUserRequest.json",
                        (Consumer<RegisterUserDTO>) request -> request.setEmail("not-an-email")
                ),
                Arguments.of(
                        "password is null",
                        "registerUserRequest.json",
                        (Consumer<RegisterUserDTO>) request -> request.setPassword(null)
                )
        );
    }

    @ParameterizedTest(name = "Should reject registration when {0}")
    @MethodSource("invalidRegisterUserRequests")
    void givenInvalidRequest_whenRegisterUser_thenBadRequestAndNoUserPersisted(
            final String testCase,
            final String requestResource,
            final Consumer<RegisterUserDTO> requestMutator
    ) {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserBadRequest())
                .withRequest(requestResource, requestMutator)
                .expectStatus(HttpStatus.BAD_REQUEST)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(0);
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should reject registration when role is unknown")
    void givenUnknownRole_whenRegisterUser_thenBadRequestAndNoUserPersisted() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserBadRequest())
                .withRequest("registerUserRequest_invalidRole.json")
                .expectStatus(HttpStatus.BAD_REQUEST)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(0);
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should reject registration when siteId is not a UUID")
    void givenInvalidSiteId_whenRegisterUser_thenBadRequestAndNoUserPersisted() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserBadRequest())
                .withRequest("registerUserRequest_invalidSiteId.json")
                .expectStatus(HttpStatus.BAD_REQUEST)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(0);
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should reject registration when siteId is missing for site-scoped role")
    void givenSiteScopedRoleAndMissingSiteId_whenRegisterUser_thenBadRequestAndNoUserPersisted() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserBadRequest())
                .withRequest("registerUserRequest.json", (Consumer<RegisterUserDTO>) request -> request.setSiteId(null))
                .expectStatus(HttpStatus.BAD_REQUEST)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(0);
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should allow registration for global role without siteId")
    void givenGlobalRoleAndMissingSiteId_whenRegisterUser_thenCreatedAndUserPersisted() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json", (Consumer<RegisterUserDTO>) request -> {
                    request.setEmail("global-admin@sitionix.com");
                    request.setRole(RegisterUserDTO.RoleEnum.SUPER_ADMIN);
                    request.setSiteId(null);
                })
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "id", "passwordHash", "updatedAt")
                .containsWithJsonsStrict("registeredUserGlobalEntity.json");
    }

    @Test
    @DisplayName("Should ignore siteId for global role registration")
    void givenGlobalRoleWithSiteId_whenRegisterUser_thenCreatedAndSiteIdIgnored() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json", (Consumer<RegisterUserDTO>) request -> {
                    request.setEmail("global-admin@sitionix.com");
                    request.setRole(RegisterUserDTO.RoleEnum.SUPER_ADMIN);
                })
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "id", "passwordHash", "updatedAt")
                .containsWithJsonsStrict("registeredUserGlobalEntity.json");
    }

    @Test
    @DisplayName("Should allow same email in different sites for site-scoped roles")
    void givenExistingEmailDifferentSiteId_whenRegisterUser_thenRegistrationSucceeds() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest_sameEmailDifferentSiteId.json")
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(2);
    }

    @Test
    @DisplayName("Should return 201 and resend verification when pending user re-registers in same site")
    void givenPendingEmailSameSite_whenRegisterUser_thenCreatedAndResendVerification() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest_duplicateEmailSameSite.json")
                .expectResponse("registerUserResponse_pendingEmailVerify.json", "userId")
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1);
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "nextRetryAt", "payload", "createdAt", "updatedAt", "aggregateId")
                .containsWithJsonsStrict("outboxEventEmailVerifyEntity.json");
    }

    @Test
    @DisplayName("Should return 201 without resend when pending user is on cooldown")
    void givenPendingEmailSameSiteWithRecentToken_whenRegisterUser_thenCreatedWithoutResend() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenRecent.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest_duplicateEmailSameSite.json")
                .expectResponse("registerUserResponse_pendingEmailVerify.json", "userId")
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1);
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should return 409 Conflict when email is already registered in the same site for site-scoped roles")
    void givenExistingEmailSameSite_whenRegisterUser_thenConflict() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserConflict())
                .withRequest("registerUserRequest_duplicateEmailSameSite.json")
                .expectResponse("registerUserResponse_duplicateEmail.json", r -> r.setDetails("Email already registered for this role and context"))
                .expectStatus(HttpStatus.CONFLICT)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1);
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should return 409 Conflict when email is already registered for global roles")
    void givenExistingEmailGlobalRole_whenRegisterUser_thenConflict() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserConflict())
                .withRequest("registerUserRequest_duplicateEmailGlobalRole.json")
                .expectResponse("registerUserResponse_duplicateEmail.json", r -> r.setDetails("Email already registered for this role and context"))
                .expectStatus(HttpStatus.CONFLICT)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1);
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should return 400 Bad Request with password policy message when password format is invalid")
    void givenInvalidPassword_whenRegisterUser_thenBadRequestWithDetails() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserBadRequest())
                .withRequest("registerUserRequest.json", (Consumer<RegisterUserDTO>) request -> request.setPassword("weak"))
                .expectResponse("registerUserResponse_invalidPassword.json")
                .expectStatus(HttpStatus.BAD_REQUEST)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(0);
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    private String getDefaultPassword() {
        return "StrongPassword123";
    }
}
