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
class AuthControllerIT extends InternalAuthITSupport {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should login successfully and persist refresh token")
    void given_active_user_when_login_then_ok_and_refresh_token_saved() {
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
                .token("Bearer " + this.serviceToken)
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
    @DisplayName("Should forbid login when notification service calls auth endpoint")
    void given_notification_service_when_login_then_forbidden() {
        //given
        final String token = this.buildServiceToken("notificationservice-sox");

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .token("Bearer " + token)
                .withRequest("loginRequest.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should forbid login when unknown service calls auth endpoint")
    void given_unknown_service_when_login_then_forbidden() {
        //given
        final String token = this.buildServiceToken("otherservice-sox");

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .token("Bearer " + token)
                .withRequest("loginRequest.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void given_invalid_credentials_when_login_then_unauthorized() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_same_session_when_login_twice_then_old_refresh_token_rejected_and_single_active_token() {
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
                .token("Bearer " + this.serviceToken)
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .andExpectPath(result -> refreshTokens.add(JsonPath.read(result.getResponse().getContentAsString(),
                        "$.refreshToken")))
                .assertAndCreate();

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .token("Bearer " + this.serviceToken)
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .token("Bearer " + this.serviceToken)
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
    void given_login_request_when_login_then_password_not_logged() {
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
                    .token("Bearer " + this.serviceToken)
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
    void given_active_session_when_login_again_then_reuse_session_and_issue_new_refresh_token() {
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
                .token("Bearer " + this.serviceToken)
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .token("Bearer " + this.serviceToken)
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
    void given_active_user_and_new_device_when_login_then_create_second_session_and_refresh_token() {
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
                .token("Bearer " + this.serviceToken)
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .token("Bearer " + this.serviceToken)
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
    void given_inactive_session_when_login_then_reactivate_session_and_issue_refresh_token() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_inactive_user_when_login_then_unauthorized() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_pending_user_when_login_then_unauthorized_and_no_refresh_token_saved() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_banned_user_when_login_then_unauthorized_and_no_refresh_token_saved() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_unknown_user_when_login_then_unauthorized() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginUnauthorized())
                .token("Bearer " + this.serviceToken)
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
    void given_site_and_global_users_same_email_when_login_with_site_id_then_use_site_user() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_invalid_email_when_login_then_bad_request() {
        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginBadRequest())
                .token("Bearer " + this.serviceToken)
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
    void given_site_scoped_user_and_missing_site_id_when_login_then_bad_request() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_global_user_and_missing_site_id_when_login_then_ok_and_refresh_token_saved() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_active_user_when_login_then_access_token_has_expected_headers_and_claims() throws Exception {
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
                .token("Bearer " + this.serviceToken)
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
    void given_suspicious_session_when_login_then_session_becomes_active_and_token_saved() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_concurrent_logins_with_same_session_source_id_when_login_then_single_session_persisted() throws InterruptedException {
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
                        .token("Bearer " + this.serviceToken)
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
    void given_valid_login_when_login_then_response_contains_only_tokens_and_no_cookies() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_pending_user_and_valid_token_when_verify_email_then_ok_and_user_activated_and_token_used() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_pending_user_and_missing_site_id_when_verify_email_then_accepted_and_no_changes() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_pending_global_user_and_global_token_when_verify_email_then_ok_and_user_activated_and_token_used() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_active_user_and_used_token_when_verify_email_then_accepted_and_no_changes() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_pending_user_and_expired_token_when_verify_email_then_accepted_and_no_changes() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_pending_user_and_unknown_token_when_verify_email_then_accepted_and_no_changes() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_pending_user_and_mismatched_site_when_verify_email_then_accepted_and_no_changes() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_pending_user_and_revoked_token_when_verify_email_then_accepted_and_no_changes() {
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
                .token("Bearer " + this.serviceToken)
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
    void given_missing_token_when_verify_email_then_bad_request() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailBadRequest())
                .token("Bearer " + this.serviceToken)
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> request.setToken(null))
                .expectStatus(HttpStatus.BAD_REQUEST)
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should reject verification when siteId is missing")
    void given_missing_site_id_when_verify_email_then_bad_request() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.verifyEmailBadRequest())
                .token("Bearer " + this.serviceToken)
                .withRequest("verifyEmailRequest.json", (EmailVerificationDTO request) -> request.setSiteId(null))
                .expectStatus(HttpStatus.ACCEPTED)
                .assertAndCreate();

        //then
    }

}
