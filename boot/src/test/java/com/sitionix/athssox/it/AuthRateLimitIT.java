package com.sitionix.athssox.it;

import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
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
        "auth.rate-limit.login.email.limit=2",
        "auth.rate-limit.login.email.window=1s",
        "auth.rate-limit.login.ip.limit=3",
        "auth.rate-limit.login.ip.window=1s",
        "auth.rate-limit.login.ip-session.limit=2",
        "auth.rate-limit.login.ip-session.window=1s"
})
class AuthRateLimitIT {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should return 429 and Retry-After when login email exceeds limit")
    void givenTooManyLoginAttemptsByEmail_whenLogin_thenTooManyRequests() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();
        final String email = "user@sitionix.com";
        final String sessionSourceId = "device-123";

        //when
        for (int i = 0; i < 2; i++) {
            this.testManager.mockMvc()
                    .ping(ControllerEndpoint.loginUnauthorized())
                    .withRequest("loginRequest.json", request -> {
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
                .withRequest("loginRequest.json", request -> {
                    request.setEmail(email);
                    request.setPassword("wrong-password");
                    request.setSessionSourceId(sessionSourceId);
                })
                .expectStatus(HttpStatus.TOO_MANY_REQUESTS)
                .andExpectPath(MockMvcResultMatchers.header().exists(HttpHeaders.RETRY_AFTER))
                .expectResponse("rateLimitResponse.json", "details")
                .assertAndCreate();

        //then
        try {
            Thread.sleep(1100L);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("Should enforce IP limit even with different emails")
    void givenDifferentEmailsSameIp_whenLoginTooManyTimes_thenTooManyRequests() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();

        //when
        for (int i = 1; i <= 3; i++) {
            final String email = "user-" + i + "@sitionix.com";
            final String sessionSourceId = "device-" + i;
            this.testManager.mockMvc()
                    .ping(ControllerEndpoint.loginUnauthorized())
                    .withRequest("loginRequest.json", request -> {
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
                .withRequest("loginRequest.json", request -> {
                    request.setEmail("user-4@sitionix.com");
                    request.setPassword("wrong-password");
                    request.setSessionSourceId("device-4");
                })
                .expectStatus(HttpStatus.TOO_MANY_REQUESTS)
                .andExpectPath(MockMvcResultMatchers.header().exists(HttpHeaders.RETRY_AFTER))
                .expectResponse("rateLimitResponse.json", "details")
                .assertAndCreate();

        //then
        try {
            Thread.sleep(1100L);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("Should enforce IP and session limit together")
    void givenSameIpAndSession_whenLoginTooManyTimes_thenTooManyRequests() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();
        final String sessionSourceId = "device-123";

        //when
        for (int i = 1; i <= 2; i++) {
            final String email = "session-user-" + i + "@sitionix.com";
            this.testManager.mockMvc()
                    .ping(ControllerEndpoint.loginUnauthorized())
                    .withRequest("loginRequest.json", request -> {
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
                .withRequest("loginRequest.json", request -> {
                    request.setEmail("session-user-3@sitionix.com");
                    request.setPassword("wrong-password");
                    request.setSessionSourceId(sessionSourceId);
                })
                .expectStatus(HttpStatus.TOO_MANY_REQUESTS)
                .andExpectPath(MockMvcResultMatchers.header().exists(HttpHeaders.RETRY_AFTER))
                .expectResponse("rateLimitResponse.json", "details")
                .assertAndCreate();

        //then
        try {
            Thread.sleep(1100L);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
