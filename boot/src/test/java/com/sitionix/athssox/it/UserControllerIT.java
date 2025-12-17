package com.sitionix.athssox.it;

import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.sitionix.athssox.postgresql.entity.UserEntity;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@IntegrationTest
class UserControllerIT {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should register a new user successfully and persist it")
    void givenValidUserData_whenRegisterUser_thenSuccessAndUserPersisted() {
        //given
        final UUID expectedSiteId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        final String rawPassword = "StrongPassword123";

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUser())
                .request("registerUserRequest.json")
                .response("registerUserResponse.json", "userId")
                .status(HttpStatus.CREATED)
                .createAndAssert();

        //then
        final List<UserEntity> users = this.testManager.postgresql().get(UserEntity.class).getAll();
        assertEquals(1, users.size());

        final UserEntity user = users.getFirst();
        assertEquals("email@sitionix.com", user.getEmail());
        assertThat(user.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().matches(rawPassword, user.getPasswordHash())).isTrue();
        assertEquals(expectedSiteId, user.getSiteId());
        assertThat(user.getStatus().getId()).isEqualTo(1L);
        assertThat(user.getGlobalRole().getId()).isEqualTo(1L);
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
                .request(requestResource, requestMutator)
                .status(HttpStatus.BAD_REQUEST)
                .createAndAssert();

        //then
        final List<UserEntity> users = this.testManager.postgresql().get(UserEntity.class).getAll();
        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("Should reject registration when role is unknown")
    void givenUnknownRole_whenRegisterUser_thenBadRequestAndNoUserPersisted() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserBadRequest())
                .request("registerUserRequest_invalidRole.json")
                .status(HttpStatus.BAD_REQUEST)
                .createAndAssert();

        //then
        final List<UserEntity> users = this.testManager.postgresql().get(UserEntity.class).getAll();
        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("Should reject registration when siteId is not a UUID")
    void givenInvalidSiteId_whenRegisterUser_thenBadRequestAndNoUserPersisted() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserBadRequest())
                .request("registerUserRequest_invalidSiteId.json")
                .status(HttpStatus.BAD_REQUEST)
                .createAndAssert();

        //then
        final List<UserEntity> users = this.testManager.postgresql().get(UserEntity.class).getAll();
        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("Should allow same email in different sites for site-scoped roles")
    void givenExistingEmailDifferentSiteId_whenRegisterUser_thenRegistrationSucceeds() {
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
                .request("registerUserRequest_sameEmailDifferentSiteId.json")
                .status(HttpStatus.CREATED)
                .createAndAssert();

        //then
        final List<UserEntity> users = this.testManager.postgresql().get(UserEntity.class).getAll();

        assertThat(users).hasSize(2);
    }

    @Test
    @DisplayName("Should return 409 Conflict when email is already registered in the same site for site-scoped roles")
    void givenExistingEmailSameSite_whenRegisterUser_thenConflict() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserConflict())
                .request("registerUserRequest_duplicateEmailSameSite.json")
                .response("registerUserResponse_duplicateEmail.json", r -> r.setDetails("Email already registered for this site."))
                .status(HttpStatus.CONFLICT)
                .createAndAssert();

        //then
        final List<UserEntity> users = this.testManager.postgresql().get(UserEntity.class).getAll();
        assertThat(users).hasSize(1);
    }

    @Test
    @DisplayName("Should return 409 Conflict when email is already registered for global roles")
    void givenExistingEmailGlobalRole_whenRegisterUser_thenConflict() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserConflict())
                .request("registerUserRequest_duplicateEmailGlobalRole.json")
                .response("registerUserResponse_duplicateEmail.json", r -> r.setDetails("Email already registered for this role scope."))
                .status(HttpStatus.CONFLICT)
                .createAndAssert();

        //then
        final List<UserEntity> users = this.testManager.postgresql().get(UserEntity.class).getAll();
        assertThat(users).hasSize(1);
    }

    @Test
    @DisplayName("Should return 400 Bad Request with password policy message when password format is invalid")
    void givenInvalidPassword_whenRegisterUser_thenBadRequestWithDetails() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.registerUserBadRequest())
                .request("registerUserRequest.json", (Consumer<RegisterUserDTO>) request -> request.setPassword("weak"))
                .response("registerUserResponse_invalidPassword.json")
                .status(HttpStatus.BAD_REQUEST)
                .createAndAssert();

        //then
        final List<UserEntity> users = this.testManager.postgresql().get(UserEntity.class).getAll();
        assertThat(users).isEmpty();
    }
}
