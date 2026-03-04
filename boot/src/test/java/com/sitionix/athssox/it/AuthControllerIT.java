package com.sitionix.athssox.it;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.app_afesox.athssox.api_first.dto.EmailVerificationDTO;
import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jayway.jsonpath.JsonPath;
import com.sitionix.athssox.api.controller.AuthController;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.postgresql.entity.user.UserEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Duration;
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
    @DisplayName("Should revoke old refresh token on login and keep a single active token")
    void givenSameSession_whenLoginTwice_thenOldRefreshTokenRejectedAndSingleActiveToken() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        final List<String> refreshTokens = new ArrayList<>();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .andExpectPath(result -> refreshTokens.add(JsonPath.read(result.getResponse().getContentAsString(),
                        "$.refreshToken")))
                .assertAndCreate();

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json", request -> request.setRefreshToken(refreshTokens.get(0)))
                .expectResponse("refreshAccessTokenResponse_forbidden_invalid.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(2)
                .withFetchedRelations()
                .ignoreFields("id",
                        "tokenHash",
                        "expiresAt",
                        "createdAt",
                        "updatedAt",
                        "usedAt",
                        "revokedAt",
                        "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveExpected.json",
                        "refreshTokenRevokedExpected.json");
    }

    @Test
    @DisplayName("Should not log login password values")
    void givenLoginRequest_whenLogin_thenPasswordNotLogged() {
        //given
        final String password = "plain-password";
        final Logger logger = (Logger) LoggerFactory.getLogger(AuthController.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        final boolean containsPassword;
        try {
            //when
            this.testManager.mockMvc()
                    .ping(ControllerEndpoint.loginUnauthorized())
                    .withRequest("loginRequest.json", (LoginRequestDTO request) -> request.setPassword(password))
                    .expectResponse("loginResponse_unauthorized.json")
                    .expectStatus(HttpStatus.UNAUTHORIZED)
                    .assertAndCreate();

            containsPassword = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains(password));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        //then
        assertThat(containsPassword).isFalse();
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
    void givenInactiveUser_whenLogin_thenUnauthorized() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(3L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

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
    @DisplayName("Should reject login when user is pending email verification")
    void givenPendingUser_whenLogin_thenUnauthorizedAndNoRefreshTokenSaved() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

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
    @DisplayName("Should reject login when user is banned")
    void givenBannedUser_whenLogin_thenUnauthorizedAndNoRefreshTokenSaved() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(4L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

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
    @DisplayName("Should issue access token with expected headers and claims on login")
    void givenActiveUser_whenLogin_thenAccessTokenHasExpectedHeadersAndClaims() throws Exception {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();
        final List<String> accessTokens = new ArrayList<>();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .andExpectPath(result -> accessTokens.add(JsonPath.read(
                        result.getResponse().getContentAsString(),
                        "$.accessToken")))
                .assertAndCreate();

        //then
        final List<UserEntity> users = this.testManager.postgresql().get(DatabaseContract.USER_ENTITY_DB_CONTRACT);
        assertThat(users).hasSize(1);
        assertThat(accessTokens).hasSize(1);

        final String accessToken = accessTokens.get(0);
        final DecodedJWT decoded = JWT.decode(accessToken);

        assertThat(decoded.getAlgorithm()).isEqualTo("RS256");
        assertThat(decoded.getKeyId()).isEqualTo("it-key");
        assertThat(decoded.getIssuer()).isEqualTo("athssox");
        assertThat(decoded.getSubject()).isEqualTo(users.get(0).getId().toString());
        assertThat(decoded.getIssuedAt()).isNotNull();
        assertThat(decoded.getExpiresAt()).isNotNull();
        assertThat(decoded.getClaim("role").asString()).isEqualTo("SITE_USER");
        assertThat(decoded.getClaim("siteId").asString()).isEqualTo("c9b1f3f4-12c7-11ec-82a8-0242ac130003");

        final Duration ttl = Duration.between(decoded.getIssuedAt().toInstant(), decoded.getExpiresAt().toInstant());
        assertThat(ttl.getSeconds()).isEqualTo(3600L);
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
        final CountDownLatch start = new CountDownLatch(1);
        final List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        final Runnable loginTask = () -> {
            try {
                start.await(5, TimeUnit.SECONDS);
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
            } catch (final Throwable exception) {
                failures.add(exception);
            }
        };

        //when
        final boolean terminated;
        try {
            for (int i = 0; i < attempts; i++) {
                executor.submit(loginTask);
            }
            start.countDown();
            executor.shutdown();
            terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        //then
        assertThat(terminated).isTrue();
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
