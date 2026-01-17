package com.sitionix.athssox.it;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jayway.jsonpath.JsonPath;
import com.sitionix.athssox.api.controller.AuthController;
import com.sitionix.athssox.domain.model.RefreshTokenStatus;
import com.sitionix.athssox.domain.model.SessionStatus;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.postgresql.entity.session.DeviceSessionEntity;
import com.sitionix.athssox.postgresql.entity.token.RefreshTokenEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
@IntegrationTest
@Import(AuthRefreshControllerIT.FixedClockConfig.class)
class AuthRefreshControllerIT {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should refresh access token and rotate refresh token")
    void givenValidRefreshToken_whenRefreshAccessToken_thenOkAndRotateRefreshToken() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(2)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveExpected.json", "refreshTokenRevokedExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionActiveExpected.json");
    }

    @Test
    @DisplayName("Should keep refresh tokens bound to a session and only one active after rotation")
    void givenRotatedRefreshToken_whenInspectTokens_thenSingleActiveAndAllBoundToSession() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(2)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveExpected.json", "refreshTokenRevokedExpected.json");
    }

    @Test
    @DisplayName("Should treat refresh token expiring at now as expired")
    void givenRefreshTokenExpiringAtNow_whenRefreshAccessToken_thenUnauthorized() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenExpiresAtNow.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse_unauthorized.json")
                .expectStatus(HttpStatus.UNAUTHORIZED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveExpected.json");
    }

    @Test
    @DisplayName("Should reject refresh when token is expired")
    void givenExpiredRefreshToken_whenRefreshAccessToken_thenUnauthorized() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenExpired.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json", request -> request.setRefreshToken("refresh-token-expired"))
                .expectResponse("refreshAccessTokenResponse_unauthorized.json")
                .expectStatus(HttpStatus.UNAUTHORIZED)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveExpected.json");
    }

    @Test
    @DisplayName("Should reject refresh when token is invalid")
    void givenInvalidRefreshToken_whenRefreshAccessToken_thenForbidden() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json", request -> request.setRefreshToken("refresh-token-unknown"))
                .expectResponse("refreshAccessTokenResponse_forbidden_invalid.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(0);
    }

    @Test
    @DisplayName("Should reject refresh when session is not active")
    void givenInactiveSession_whenRefreshAccessToken_thenForbidden() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(3L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionRevoked.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse_forbidden_session.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionRevokedExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveSessionRevokedExpected.json");
    }

    @Test
    @DisplayName("Should detect refresh token replay and mark session suspicious")
    void givenRevokedRefreshToken_whenRefreshAccessToken_thenForbiddenAndSessionSuspicious() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenRevoked.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json", request -> request.setRefreshToken("refresh-token-revoked"))
                .expectResponse("refreshAccessTokenResponse_forbidden_invalid.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionSuspiciousExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenRevokedSessionSuspiciousExpected.json");
    }

    @Test
    @DisplayName("Should reject refresh token reuse after rotation and mark session suspicious")
    void givenRotatedRefreshToken_whenRefreshAccessTokenAgain_thenForbiddenAndSessionSuspicious() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse_forbidden_invalid.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionSuspiciousExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(2)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveSessionSuspiciousExpected.json", "refreshTokenRevokedSessionSuspiciousExpected.json");
    }

    @Test
    @DisplayName("Should refresh token for matching session when multiple sessions exist")
    void givenMultipleSessionsAndMatchingContext_whenRefreshAccessToken_thenOkAndOnlyBoundSessionUpdated() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(UserStatus.ACTIVE.getId()))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(UserRole.SITE_USER.getId()))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))

                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(SessionStatus.ACTIVE.getId()))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActiveOther.json"))

                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(SessionStatus.ACTIVE.getId()))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))

                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(RefreshTokenStatus.ACTIVE.getId()))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse.json", "accessToken", "refreshToken")
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
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveExpected.json", "refreshTokenRevokedExpected.json");
    }

    @Test
    @DisplayName("Should reject refresh when session does not match token context")
    void givenSessionMismatch_whenRefreshAccessToken_thenForbidden() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(UserStatus.ACTIVE.getId()))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(UserRole.SITE_USER.getId()))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))

                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(SessionStatus.ACTIVE.getId()))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActiveOther.json"))

                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(SessionStatus.ACTIVE.getId()))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))

                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(RefreshTokenStatus.ACTIVE.getId()))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json", request -> request.setSessionSourceId("device-999"))
                .expectResponse("refreshAccessTokenResponse_forbidden_mismatch.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(2)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionActiveOtherExpected.json", "deviceSessionSuspiciousExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveSessionSuspiciousExpected.json");
    }

    @Test
    @DisplayName("Should reject refresh when session is suspicious")
    void givenSuspiciousSession_whenRefreshAccessToken_thenForbidden() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionSuspicious.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse_forbidden_session.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionSuspiciousExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveSessionSuspiciousExpected.json");
    }

    @Test
    @DisplayName("Should allow only one concurrent refresh for the same token")
    void givenConcurrentRefreshRequests_whenRefreshAccessToken_thenSingleSuccessAndConsistentState() throws InterruptedException {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(UserStatus.ACTIVE.getId()))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(UserRole.SITE_USER.getId()))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(SessionStatus.ACTIVE.getId()))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(RefreshTokenStatus.ACTIVE.getId()))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        final int attempts = 2;
        final ExecutorService executor = Executors.newFixedThreadPool(attempts);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Integer> statuses = Collections.synchronizedList(new ArrayList<>());
        final List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        final Runnable refreshTask = () -> {
            try {
                start.await(5, TimeUnit.SECONDS);
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
                failures.add(exception);
                return;
            }
            try {
                this.testManager.mockMvc()
                        .ping(ControllerEndpoint.refreshAccessToken())
                        .withRequest("refreshAccessTokenRequest.json")
                        .andExpectPath(result -> statuses.add(result.getResponse().getStatus()))
                        .assertAndCreate();
            } catch (final Throwable exception) {
                failures.add(exception);
            }
        };

        //when
        final boolean terminated;
        try {
            for (int i = 0; i < attempts; i++) {
                executor.submit(refreshTask);
            }
            start.countDown();
            executor.shutdown();
            terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        //then
        assertThat(terminated).isTrue();
        assertThat(statuses).hasSize(2);
        assertThat(Collections.frequency(statuses, HttpStatus.OK.value())).isEqualTo(1);
        assertThat(Collections.frequency(statuses, HttpStatus.FORBIDDEN.value())).isEqualTo(1);
        assertThat(failures).isEmpty();

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionSuspiciousExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(2)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveSessionSuspiciousExpected.json", "refreshTokenRevokedSessionSuspiciousExpected.json");
    }

    @Test
    @DisplayName("Should update session lastUsedAt only once within the throttle window")
    void givenMultipleRefreshesWithinThrottleWindow_whenRefreshAccessToken_thenUpdateLastUsedAtOnce() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(UserStatus.ACTIVE.getId()))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(UserRole.SITE_USER.getId()))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(SessionStatus.ACTIVE.getId()))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActiveOldLastUsed.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(RefreshTokenStatus.ACTIVE.getId()))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        final List<String> refreshTokens = new ArrayList<>();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .andExpectPath(result -> refreshTokens.add(JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken")))
                .assertAndCreate();

        final List<DeviceSessionEntity> sessionsAfterFirst =
                this.testManager.postgresql().get(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT);

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json", request -> request.setRefreshToken(refreshTokens.get(0)))
                .expectResponse("refreshAccessTokenResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        final List<DeviceSessionEntity> sessionsAfterSecond =
                this.testManager.postgresql().get(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT);

        //then
        assertThat(refreshTokens).hasSize(1);
        assertThat(sessionsAfterFirst).hasSize(1);
        assertThat(sessionsAfterSecond).hasSize(1);

        final Instant lastUsedAfterFirst = sessionsAfterFirst.get(0).getLastUsedAt();
        final Instant lastUsedAfterSecond = sessionsAfterSecond.get(0).getLastUsedAt();

        assertThat(lastUsedAfterFirst).isEqualTo(Instant.parse("2099-01-01T00:00:00Z"));
        assertThat(lastUsedAfterSecond).isEqualTo(lastUsedAfterFirst);
    }

    @Test
    @DisplayName("Should not log refresh token values")
    void givenRefreshToken_whenRefreshAccessToken_thenRefreshTokenNotLogged() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(UserStatus.ACTIVE.getId()))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(UserRole.SITE_USER.getId()))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(SessionStatus.ACTIVE.getId()))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(RefreshTokenStatus.ACTIVE.getId()))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        final Logger logger = (Logger) LoggerFactory.getLogger(AuthController.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        final boolean containsRefreshToken;
        try {
            //when
            this.testManager.mockMvc()
                    .ping(ControllerEndpoint.refreshAccessToken())
                    .withRequest("refreshAccessTokenRequest.json")
                    .expectResponse("refreshAccessTokenResponse.json", "accessToken", "refreshToken")
                    .expectStatus(HttpStatus.OK)
                    .assertAndCreate();

            containsRefreshToken = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("refresh-token-valid"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        //then
        assertThat(containsRefreshToken).isFalse();
    }

    @Test
    @DisplayName("Should extend refresh token expiry on rotation")
    void givenValidRefreshToken_whenRefreshAccessToken_thenSetNewRefreshTokenExpiresAt() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(UserStatus.ACTIVE.getId()))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(UserRole.SITE_USER.getId()))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(SessionStatus.ACTIVE.getId()))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(RefreshTokenStatus.ACTIVE.getId()))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        final List<RefreshTokenEntity> tokens =
                this.testManager.postgresql().get(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT);
        assertThat(tokens).hasSize(2);

        final RefreshTokenEntity activeToken = tokens.stream()
                .filter(token -> token.getStatus().getId().equals(RefreshTokenStatus.ACTIVE.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(activeToken.getExpiresAt()).isEqualTo(Instant.parse("2099-01-31T00:00:00Z"));
    }

    @Test
    @DisplayName("Should reject refresh for inactive user")
    void givenInactiveUser_whenRefreshAccessToken_thenForbiddenAndNoRotation() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(UserStatus.INACTIVE.getId()))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(UserRole.SITE_USER.getId()))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(SessionStatus.ACTIVE.getId()))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(RefreshTokenStatus.ACTIVE.getId()))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse_forbidden_user.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        final List<RefreshTokenEntity> tokens =
                this.testManager.postgresql().get(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT);
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getStatus().getId()).isEqualTo(RefreshTokenStatus.ACTIVE.getId());
    }

    @Test
    @DisplayName("Should reject refresh for banned user")
    void givenBannedUser_whenRefreshAccessToken_thenForbiddenAndNoRotation() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(UserStatus.BANNED.getId()))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(UserRole.SITE_USER.getId()))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(SessionStatus.ACTIVE.getId()))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(RefreshTokenStatus.ACTIVE.getId()))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.refreshAccessToken())
                .withRequest("refreshAccessTokenRequest.json")
                .expectResponse("refreshAccessTokenResponse_forbidden_user.json")
                .expectStatus(HttpStatus.FORBIDDEN)
                .assertAndCreate();

        //then
        final List<RefreshTokenEntity> tokens =
                this.testManager.postgresql().get(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT);
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getStatus().getId()).isEqualTo(RefreshTokenStatus.ACTIVE.getId());
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2099-01-01T00:00:00Z"), ZoneOffset.UTC);
        }
    }
}
