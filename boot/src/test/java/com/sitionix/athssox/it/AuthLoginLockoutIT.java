package com.sitionix.athssox.it;

import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@IntegrationTest
@TestPropertySource(properties = {
        "auth.rate-limit.enabled=false",
        "auth.login-lockout.enabled=true",
        "auth.login-lockout.failure-threshold=2",
        "auth.login-lockout.failure-window=2s",
        "auth.login-lockout.cooldown=1s"
})
class AuthLoginLockoutIT {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should lock login after repeated failures")
    void givenTooManyFailures_whenLogin_thenTooManyRequests() {
        //given
        final String email = "lockout-user@sitionix.com";
        final String sessionSourceId = "device-123";

        //when
        for (int i = 0; i < 2; i++) {
            this.testManager.mockMvc()
                    .ping(ControllerEndpoint.loginUnauthorized())
                    .withRequest("loginRequest.json", (LoginRequestDTO request) -> {
                        request.setEmail(email);
                        request.setPassword("wrong-password");
                        request.setSessionSourceId(sessionSourceId);
                    })
                    .expectStatus(HttpStatus.UNAUTHORIZED)
                    .expectResponse("loginResponse_unauthorized.json")
                    .assertAndCreate();
        }

        //then
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginUnauthorized())
                .withRequest("loginRequest.json", (LoginRequestDTO request) -> {
                    request.setEmail(email);
                    request.setPassword("wrong-password");
                    request.setSessionSourceId(sessionSourceId);
                })
                .expectStatus(HttpStatus.TOO_MANY_REQUESTS)
                .andExpectPath(MockMvcResultMatchers.header().exists(HttpHeaders.RETRY_AFTER))
                .expectResponse("rateLimitResponse.json", "details")
                .assertAndCreate();
    }

    @Test
    @DisplayName("Should allow login after cooldown expires")
    void givenCooldownExpired_whenLogin_thenUnauthorized() {
        //given
        final String email = "lockout-user-cooldown@sitionix.com";
        final String sessionSourceId = "device-456";

        //when
        for (int i = 0; i < 2; i++) {
            this.testManager.mockMvc()
                    .ping(ControllerEndpoint.loginUnauthorized())
                    .withRequest("loginRequest.json", (LoginRequestDTO request) -> {
                        request.setEmail(email);
                        request.setPassword("wrong-password");
                        request.setSessionSourceId(sessionSourceId);
                    })
                    .expectStatus(HttpStatus.UNAUTHORIZED)
                    .expectResponse("loginResponse_unauthorized.json")
                    .assertAndCreate();
        }

        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginUnauthorized())
                .withRequest("loginRequest.json", (LoginRequestDTO request) -> {
                    request.setEmail(email);
                    request.setPassword("wrong-password");
                    request.setSessionSourceId(sessionSourceId);
                })
                .expectStatus(HttpStatus.TOO_MANY_REQUESTS)
                .andExpectPath(MockMvcResultMatchers.header().exists(HttpHeaders.RETRY_AFTER))
                .expectResponse("rateLimitResponse.json", "details")
                .assertAndCreate();

        try {
            Thread.sleep(1100L);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        //then
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginUnauthorized())
                .withRequest("loginRequest.json", (LoginRequestDTO request) -> {
                    request.setEmail(email);
                    request.setPassword("wrong-password");
                    request.setSessionSourceId(sessionSourceId);
                })
                .expectStatus(HttpStatus.UNAUTHORIZED)
                .expectResponse("loginResponse_unauthorized.json")
                .assertAndCreate();
    }
}
