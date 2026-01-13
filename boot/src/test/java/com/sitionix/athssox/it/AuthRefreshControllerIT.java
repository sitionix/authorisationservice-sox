package com.sitionix.athssox.it;

import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@IntegrationTest
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
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActive.json"))
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
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialIpAddress", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionActiveExpected.json");
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
                .containsAllWithJsons("refreshTokenActiveSessionRevokedExpected.json");
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
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActive.json"))
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
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialIpAddress", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionRevokedExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveExpected.json");
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
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialIpAddress", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionSuspiciousExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenRevokedSessionSuspiciousExpected.json");
    }

    @Test
    @DisplayName("Should reject refresh when session does not match token context")
    void givenSessionMismatch_whenRefreshAccessToken_thenForbidden() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActiveOther.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActive.json"))
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
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActive.json"))
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
                .ignoreFields("id", "createdAt", "lastUsedAt", "initialIpAddress", "initialUserAgent", "lastUserAgent")
                .containsAllWithJsons("deviceSessionSuspiciousExpected.json");

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1)
                .withFetchedRelations()
                .ignoreFields("id", "tokenHash", "expiresAt", "createdAt", "updatedAt", "usedAt", "revokedAt", "rotatedFromTokenId")
                .containsAllWithJsons("refreshTokenActiveExpected.json");
    }
}
