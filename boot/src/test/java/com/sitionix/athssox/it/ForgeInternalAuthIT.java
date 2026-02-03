package com.sitionix.athssox.it;

import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@IntegrationTest
class ForgeInternalAuthIT {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should return unauthorized when missing internal authorization")
    void givenMissingToken_whenLogin_thenUnauthorized() {
        //given

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .withRequest("loginRequest.json")
                .expectStatus(HttpStatus.UNAUTHORIZED)
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should return unauthorized when invalid internal authorization")
    void givenInvalidToken_whenLogin_thenUnauthorized() {
        //given
        final String invalidToken = "Bearer invalid";

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.login())
                .token(invalidToken)
                .withRequest("loginRequest.json")
                .expectStatus(HttpStatus.UNAUTHORIZED)
                .assertAndCreate();

        //then
    }
}
