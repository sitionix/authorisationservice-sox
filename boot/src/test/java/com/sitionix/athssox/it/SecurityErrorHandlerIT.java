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
class SecurityErrorHandlerIT extends InternalAuthITSupport {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should return error body for invalid credentials on login")
    void given_invalid_credentials_when_login_then_unauthorized_with_body() {
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
                .withRequest("loginRequest.json", request -> request.setPassword("wrong-password"))
                .expectResponse("loginResponse_unauthorized.json")
                .expectStatus(HttpStatus.UNAUTHORIZED)
                .assertAndCreate();
    }
}
