package com.sitionix.athssox.it;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.sitionix.athssox.api.controller.UserController;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.postgresql.entity.user.UserEntity;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxEventEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
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
    void given_global_role_without_site_id_when_register_user_then_success_and_user_persisted() {
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
    void given_site_admin_role_without_site_id_when_register_user_then_return_bad_request() {
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
    void given_valid_user_data_when_register_user_then_success_and_user_persisted() {
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
        assertThat(persistedUser.getPasswordHash()).isNotEqualTo("StrongPassword123");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "nextRetryAt", "payload", "createdAt", "updatedAt", "aggregateId")
                .containsWithJsonsStrict("outboxEventEmailVerifyEntity.json");
    }

    @Test
    @DisplayName("Should not store raw verification token or verifyUrl in outbox payload")
    void given_registration_when_user_created_then_outbox_payload_has_no_raw_token() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .withRequest("registerUserRequest.json")
                .expectResponse("registerUserResponse.json", "userId")
                .expectStatus(HttpStatus.CREATED)
                .assertAndCreate();

        //then
        final List<OutboxEventEntity> events =
                this.testManager.postgresql().get(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT);
        assertThat(events).hasSize(1);
        final String payload = events.get(0).getPayload();
        assertThat(payload).doesNotContain("token=");
        assertThat(payload).doesNotContain("verifyUrl");
        assertThat(payload).contains("emailVerificationTokenId");
        assertThat(payload).contains("pepperId");
    }

    @Test
    @DisplayName("Should not log registration password or verification token")
    void given_registration_request_when_register_user_then_sensitive_data_not_logged() {
        //given
        final String password = "StrongPassword123";
        final Logger logger = (Logger) LoggerFactory.getLogger(UserController.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        final boolean containsPassword;
        final boolean containsVerifyToken;
        try {
            //when
            this.testManager.mockMvc()
                    .ping(ControllerEndpoint.registerUser())
                    .withRequest("registerUserRequest.json", (RegisterUserDTO request) -> request.setPassword(password))
                    .expectResponse("registerUserResponse.json", "userId")
                    .expectStatus(HttpStatus.CREATED)
                    .assertAndCreate();

            containsPassword = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains(password));
            containsVerifyToken = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("token="));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        //then
        assertThat(containsPassword).isFalse();
        assertThat(containsVerifyToken).isFalse();
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
    void given_invalid_request_when_register_user_then_bad_request_and_no_user_persisted(
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
    void given_unknown_role_when_register_user_then_bad_request_and_no_user_persisted() {
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
    void given_invalid_site_id_when_register_user_then_bad_request_and_no_user_persisted() {
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
    void given_site_scoped_role_and_missing_site_id_when_register_user_then_bad_request_and_no_user_persisted() {
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
    void given_global_role_and_missing_site_id_when_register_user_then_created_and_user_persisted() {
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
    void given_global_role_with_site_id_when_register_user_then_created_and_site_id_ignored() {
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
    void given_existing_email_different_site_id_when_register_user_then_registration_succeeds() {
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
    void given_pending_email_same_site_when_register_user_then_created_and_resend_verification() {
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
    void given_pending_email_same_site_with_recent_token_when_register_user_then_created_without_resend() {
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
    void given_existing_email_same_site_when_register_user_then_conflict() {
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
                .expectResponse("registerUserResponse_duplicateEmail.json", r -> r.setDetails("Registration already processed. Please check your email."))
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
    void given_existing_email_global_role_when_register_user_then_conflict() {
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
                .expectResponse("registerUserResponse_duplicateEmail.json", r -> r.setDetails("Registration already processed. Please check your email."))
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
    void given_invalid_password_when_register_user_then_bad_request_with_details() {
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
}
