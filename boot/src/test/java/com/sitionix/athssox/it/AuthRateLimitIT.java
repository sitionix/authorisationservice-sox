package com.sitionix.athssox.it;

import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@TestPropertySource(properties = {
        "auth.rate-limit.login.email.limit=5",
        "auth.rate-limit.login.email.window=2s",
        "auth.rate-limit.login.ip.limit=20",
        "auth.rate-limit.login.ip.window=2s",
        "auth.rate-limit.login.ip-session.limit=10",
        "auth.rate-limit.login.ip-session.window=2s"
})
class AuthRateLimitIT {

    @Autowired
    private TestManager testManager;

    @AfterEach
    void tearDown() {
        this.awaitWindowReset();
    }

    @Test
    @DisplayName("Should return 429 and Retry-After when login email exceeds limit")
    void givenTooManyLoginAttemptsByEmail_whenLogin_thenTooManyRequests() {
        //given
        this.setupActiveUser();
        final String email = "user@sitionix.com";
        final String sessionSourceId = "device-123";
        final AtomicReference<String> retryAfter = new AtomicReference<>();

        //when
        this.performUnauthorizedLogins(email, sessionSourceId, 5);
        this.loginTooMany(email, sessionSourceId, retryAfter);

        //then
        assertThat(retryAfter.get()).isNotBlank();
        assertThat(Long.parseLong(retryAfter.get())).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Should enforce IP limit even with different emails")
    void givenDifferentEmailsSameIp_whenLoginTooManyTimes_thenTooManyRequests() {
        //given
        this.setupActiveUser();
        final List<String> emails = this.getEmails("user", 21);
        final List<String> sessionSourceIds = this.getSessionSourceIds("device", 21);
        final AtomicReference<String> retryAfter = new AtomicReference<>();

        //when
        for (int i = 0; i < emails.size() - 1; i++) {
            this.loginUnauthorized(emails.get(i), sessionSourceIds.get(i));
        }
        this.loginTooMany(emails.get(emails.size() - 1),
                sessionSourceIds.get(sessionSourceIds.size() - 1),
                retryAfter);

        //then
        assertThat(retryAfter.get()).isNotBlank();
    }

    @Test
    @DisplayName("Should enforce IP and session limit together")
    void givenSameIpAndSession_whenLoginTooManyTimes_thenTooManyRequests() {
        //given
        this.setupActiveUser();
        final String sessionSourceId = "device-123";
        final List<String> emails = this.getEmails("session-user", 11);
        final AtomicReference<String> retryAfter = new AtomicReference<>();

        //when
        for (int i = 0; i < emails.size() - 1; i++) {
            this.loginUnauthorized(emails.get(i), sessionSourceId);
        }
        this.loginTooMany(emails.get(emails.size() - 1), sessionSourceId, retryAfter);

        //then
        assertThat(retryAfter.get()).isNotBlank();
    }

    private void setupActiveUser() {
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .build();
    }

    private void performUnauthorizedLogins(final String email, final String sessionSourceId, final int count) {
        for (int i = 0; i < count; i++) {
            this.loginUnauthorized(email, sessionSourceId);
        }
    }

    private void loginUnauthorized(final String email, final String sessionSourceId) {
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

    private void loginTooMany(final String email, final String sessionSourceId, final AtomicReference<String> retryAfter) {
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
                .andExpectPath(result -> {
                    retryAfter.set(result.getResponse().getHeader(HttpHeaders.RETRY_AFTER));
                })
                .assertAndCreate();
    }

    private List<String> getEmails(final String prefix, final int count) {
        final List<String> emails = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            emails.add(prefix + "-" + i + "@sitionix.com");
        }
        return emails;
    }

    private List<String> getSessionSourceIds(final String prefix, final int count) {
        final List<String> sessionSourceIds = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            sessionSourceIds.add(prefix + "-" + i);
        }
        return sessionSourceIds;
    }

    private void awaitWindowReset() {
        try {
            Thread.sleep(2200L);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
