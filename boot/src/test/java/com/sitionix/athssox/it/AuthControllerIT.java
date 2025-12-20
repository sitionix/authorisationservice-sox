package com.sitionix.athssox.it;

import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.athssox.postgresql.entity.RefreshTokenEntity;
import com.sitionix.athssox.postgresql.entity.UserEntity;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;

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
        final UserEntity createdUser = this.testManager.postgresql()
                .get(UserEntity.class)
                .getAll()
                .getFirst();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectResponse("loginResponse.json", "accessToken", "refreshToken")
                .expectStatus(HttpStatus.OK)
                .assertAndCreate();

        //then
        final List<RefreshTokenEntity> refreshTokens = this.testManager.postgresql()
                .get(RefreshTokenEntity.class)
                .getAll();

        assertThat(refreshTokens).hasSize(1);

        final RefreshTokenEntity refreshToken = refreshTokens.getFirst();

        assertThat(refreshToken.getUser().getId()).isEqualTo(createdUser.getId());
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
        assertThat(this.testManager.postgresql()
                .get(RefreshTokenEntity.class)
                .getAll()).isEmpty();
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
        assertThat(this.testManager.postgresql()
                .get(RefreshTokenEntity.class)
                .getAll()).isEmpty();
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
        assertThat(this.testManager.postgresql()
                .get(RefreshTokenEntity.class)
                .getAll()).isEmpty();
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
        assertThat(this.testManager.postgresql()
                .get(RefreshTokenEntity.class)
                .getAll()).isEmpty();
    }
}
