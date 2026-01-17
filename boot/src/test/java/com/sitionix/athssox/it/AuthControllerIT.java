package com.sitionix.athssox.it;

import com.app_afesox.athssox.api_first.dto.EmailVerificationDTO;
import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


@IntegrationTest
class AuthControllerIT {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should login successfully and persist refresh token")
    void givenActiveUser_whenLogin_thenOkAndRefreshTokenSaved() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash", "deviceSessions", "refreshTokens")
                .containsWithJsonsStrict("authUserActiveStatusEntity.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt")
                .containsAllWithJsons("refreshTokenEntityExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionActiveExpected.json");
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void givenInvalidCredentials_whenLogin_thenUnauthorized() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginUnauthorized())
                .withRequest("loginRequest.json", (LoginRequestDTO request) -> request.setPassword("wrong-password"))
                .expectResponse("loginResponse_unauthorized.json")
                .expectStatus(HttpStatus.UNAUTHORIZED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should reuse active device session when logging in again")
    void givenActiveSession_whenLoginAgain_thenReuseSessionAndIssueNewRefreshToken() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionActiveExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(2)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt")
                .containsAllWithJsons("refreshTokenEntityExpected.json");
    }

    @Test
    @DisplayName("Should create second session when logging in from new device")
    void givenActiveUserAndNewDevice_whenLogin_thenCreateSecondSessionAndRefreshToken() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json", request -> request.setSessionSourceId("device-999"))
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(2)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionActiveExpected.json", "deviceSessionActiveOtherExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(2)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt")
                .containsAllWithJsons("refreshTokenEntityExpected.json", "refreshTokenEntityExpectedOther.json");
    }

    @Test
    @DisplayName("Should reactivate session when logging in with inactive session")
    void givenInactiveSession_whenLogin_thenReactivateSessionAndIssueRefreshToken() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(3L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionRevoked.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionActiveExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt")
                .containsAllWithJsons("refreshTokenEntityExpected.json");
    }

    @Test
    @DisplayName("Should reject login when user is inactive")
    void givenInactiveUser_whenLogin_thenForbidden() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(3L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginForbidden())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse_forbidden.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should reject login when user is pending email verification")
    void givenPendingUser_whenLogin_thenForbiddenAndNoRefreshTokenSaved() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginForbidden())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse_forbidden.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should reject login when user is banned")
    void givenBannedUser_whenLogin_thenForbiddenAndNoRefreshTokenSaved() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(4L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginForbidden())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse_forbidden.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should reject login when user is not found")
    void givenUnknownUser_whenLogin_thenUnauthorized() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginUnauthorized())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse_unauthorized.json")
                .expectStatus(HttpStatus.UNAUTHORIZED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should prefer site-scoped user when both site and global users share email")
    void givenSiteAndGlobalUsersSameEmail_whenLoginWithSiteId_thenUseSiteUser() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActiveGlobalSameEmail.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionActiveExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt")
                .containsAllWithJsons("refreshTokenEntityExpected.json");
    }

    @Test
    @DisplayName("Should reject login when email is invalid")
    void givenInvalidEmail_whenLogin_thenBadRequest() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginBadRequest())
                .withRequest("loginRequest.json", (LoginRequestDTO request) -> request.setEmail("not-an-email"))
                .expectStatus(HttpStatus.BAD_REQUEST)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should reject login when siteId is missing for site-scoped user")
    void givenSiteScopedUserAndMissingSiteId_whenLogin_thenBadRequest() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginBadRequest())
                .withRequest("loginRequest.json", (LoginRequestDTO request) -> request.setSiteId(null))
                .expectStatus(HttpStatus.BAD_REQUEST)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should allow login for global user without siteId")
    void givenGlobalUserAndMissingSiteId_whenLogin_thenOkAndRefreshTokenSaved() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActiveGlobal.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json", (LoginRequestDTO request) -> {
                    request.setEmail("global-user@sitionix.com");
                    request.setSiteId(null);
                })
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1);
    }

    @Test
    @DisplayName("Should reactivate suspicious session on login")
    void givenSuspiciousSession_whenLogin_thenSessionBecomesActiveAndTokenSaved() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionSuspicious.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionActiveExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt")
                .containsAllWithJsons("refreshTokenEntityExpected.json");
    }

    @Test
    @DisplayName("Should avoid duplicate session when concurrent logins use same sessionSourceId")
    void givenConcurrentLoginsWithSameSessionSourceId_whenLogin_thenSingleSessionPersisted() throws InterruptedException {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        final int attempts = 2;
        final ExecutorService executor = Executors.newFixedThreadPool(attempts);
        final CountDownLatch ready = new CountDownLatch(attempts);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Boolean> successes = Collections.synchronizedList(new ArrayList<>());
        final List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        final Runnable loginTask = () -> {
            ready.countDown();
            try {
                if (!start.await(5, TimeUnit.SECONDS)) {
                    failures.add(new IllegalStateException("Concurrent login start timed out"));
                    return;
                }
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
                failures.add(exception);
                return;
            }
            try {
                this.testManager.mockMvc()
                        .ping(ControllerEndpoint.login())
                        .withRequest("loginRequest.json")
                        .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                        .expectStatus(HttpStatus.OK)
                        .assertAndCreate();
                successes.add(Boolean.TRUE);
            } catch (final Throwable exception) {
                failures.add(exception);
            }
        };

        //when
        final boolean readyToStart;
        final boolean terminated;
        try {
            for (int i = 0; i < attempts; i++) {
                executor.submit(loginTask);
            }
            readyToStart = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            executor.shutdown();
            terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        //then
        assertThat(readyToStart).isTrue();
        assertThat(terminated).isTrue();
        assertThat(successes).isNotEmpty();
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1);

        final List<?> refreshTokens =
                this.testManager.postgresql().get(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT);
        assertThat(refreshTokens).hasSizeBetween(1, 2);
        assertThat(failures).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should return only token fields and no cookies on login")
    void givenValidLogin_whenLogin_thenResponseContainsOnlyTokensAndNoCookies() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.*", Matchers.hasSize(4)))
                .andExpectPath(MockMvcResultMatchers.header().doesNotExist("Set-Cookie"))
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1);
    }

    @Test
    @DisplayName("Should verify email successfully and activate user")
    void givenPendingUserAndValidToken_whenVerifyEmail_thenOkAndUserActivatedAndTokenUsed() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenValid.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailOk())
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> request.setToken("verify-token-valid"))
                .expectResponse("verifyEmailResponse_ok.json")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash")
                .containsWithJsonsStrict("authUserActiveStatusEntity.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "user", "usedAt")
                .containsWithJsonsStrict("emailVerificationTokenUsedAfterVerifyExpected.json");
    }

    @Test
    @DisplayName("Should accept verification when siteId is missing for site-scoped token")
    void givenPendingUserAndMissingSiteId_whenVerifyEmail_thenAcceptedAndNoChanges() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenValid.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailAccepted())
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> {
                    request.setToken("verify-token-valid");
                    request.setSiteId(null);
                })
                .expectResponse("verifyEmailResponse_accepted.json")
                .expectStatus(HttpStatus.ACCEPTED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash")
                .containsWithJsonsStrict("authUserPendingEmailVerifyEntity.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "user")
                .containsWithJsonsStrict("emailVerificationTokenValidExpected.json");
    }

    @Test
    @DisplayName("Should verify email for global token without siteId")
    void givenPendingGlobalUserAndGlobalToken_whenVerifyEmail_thenOkAndUserActivatedAndTokenUsed() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserPendingGlobal.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenGlobal.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailOk())
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> {
                    request.setToken("verify-token-global");
                    request.setSiteId(null);
                })
                .expectResponse("verifyEmailResponse_ok.json")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash")
                .containsWithJsonsStrict("authUserActiveGlobalStatusEntity.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "user", "usedAt")
                .containsWithJsonsStrict("emailVerificationTokenGlobalUsedExpected.json");
    }

    @Test
    @DisplayName("Should accept repeated verification attempt for active user")
    void givenActiveUserAndUsedToken_whenVerifyEmail_thenAcceptedAndNoChanges() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenUsed.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailAccepted())
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> request.setToken("verify-token-used"))
                .expectResponse("verifyEmailResponse_accepted.json")
                .expectStatus(HttpStatus.ACCEPTED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash")
                .containsWithJsonsStrict("authUserActiveStatusEntity.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "user")
                .containsWithJsonsStrict("emailVerificationTokenUsedExpected.json");
    }

    @Test
    @DisplayName("Should accept verification attempt with expired token")
    void givenPendingUserAndExpiredToken_whenVerifyEmail_thenAcceptedAndNoChanges() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenExpired.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailAccepted())
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> request.setToken("verify-token-expired"))
                .expectResponse("verifyEmailResponse_accepted.json")
                .expectStatus(HttpStatus.ACCEPTED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash")
                .containsWithJsonsStrict("authUserPendingEmailVerifyEntity.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "user")
                .containsWithJsonsStrict("emailVerificationTokenExpiredExpected.json");
    }

    @Test
    @DisplayName("Should accept verification attempt with unknown token")
    void givenPendingUserAndUnknownToken_whenVerifyEmail_thenAcceptedAndNoChanges() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailAccepted())
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> request.setToken("verify-token-unknown"))
                .expectResponse("verifyEmailResponse_accepted.json")
                .expectStatus(HttpStatus.ACCEPTED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash")
                .containsWithJsonsStrict("authUserPendingEmailVerifyEntity.json");
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should accept verification attempt when token belongs to different site")
    void givenPendingUserAndMismatchedSite_whenVerifyEmail_thenAcceptedAndNoChanges() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenSiteMismatch.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailAccepted())
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> request.setToken("verify-token-mismatch"))
                .expectResponse("verifyEmailResponse_accepted.json")
                .expectStatus(HttpStatus.ACCEPTED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash")
                .containsWithJsonsStrict("authUserPendingEmailVerifyEntity.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "user")
                .containsWithJsonsStrict("emailVerificationTokenSiteMismatchExpected.json");
    }

    @Test
    @DisplayName("Should accept verification attempt with revoked token")
    void givenPendingUserAndRevokedToken_whenVerifyEmail_thenAcceptedAndNoChanges() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(3L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenRevoked.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailAccepted())
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> request.setToken("verify-token-revoked"))
                .expectResponse("verifyEmailResponse_accepted.json")
                .expectStatus(HttpStatus.ACCEPTED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.USER_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "updatedAt", "id", "passwordHash")
                .containsWithJsonsStrict("authUserPendingEmailVerifyEntity.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("createdAt", "user")
                .containsWithJsonsStrict("emailVerificationTokenRevokedExpected.json");
    }

    @Test
    @DisplayName("Should reject verification when token is missing")
    void givenMissingToken_whenVerifyEmail_thenBadRequest() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailBadRequest())
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> request.setToken(null))
                .expectStatus(HttpStatus.BAD_REQUEST)
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should reject verification when siteId is missing")
    void givenMissingSiteId_whenVerifyEmail_thenBadRequest() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailBadRequest())
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> request.setSiteId(null))
                .expectStatus(HttpStatus.ACCEPTED)
                .assertAndCreate();

        //then
    }
}
