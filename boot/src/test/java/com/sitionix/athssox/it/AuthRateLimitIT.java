package com.sitionix.athssox.it;

import com.sitionix.athssox.api.ratelimit.EmailNormalizer;
import com.sitionix.athssox.api.ratelimit.RateLimitProperties;
import com.sitionix.athssox.domain.service.RateLimiterService;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Import(AuthRateLimitIT.MutableClockConfig.class)
class AuthRateLimitIT {

    @Autowired
    private TestManager testManager;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    @Autowired
    private MutableClock mutableClock;

    private final Set<String> usedEmails = new HashSet<>();
    private String clientIp;

    @AfterEach
    void tearDown() {
        if (this.clientIp != null) {
            this.rateLimiterService.reset("login:ip:" + this.clientIp);
        }
        for (final String email : this.usedEmails) {
            final String normalized = this.emailNormalizer.normalize(email);
            if (normalized != null) {
                this.rateLimiterService.reset("login:email:" + normalized);
            }
        }
    }

    @Test
    @DisplayName("Should return 429 and Retry-After when login email exceeds limit")
    void givenTooManyLoginAttemptsByEmail_whenLogin_thenTooManyRequests() {
        //given
        this.setupActiveUser();
        this.configureLoginLimits(10L, 2L, Duration.ofSeconds(5));
        final String email = "user@sitionix.com";
        final AtomicReference<String> retryAfter = new AtomicReference<>();

        //when
        this.loginUnauthorized(email);
        this.loginUnauthorized(email);
        this.loginTooMany(email, retryAfter);

        //then
        assertThat(retryAfter.get()).isNotBlank();
        assertThat(Long.parseLong(retryAfter.get())).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Should allow login after rate limit window elapses")
    void givenExceededRateLimit_whenWindowElapses_thenLoginReturnsUnauthorized() {
        //given
        this.setupActiveUser();
        this.configureLoginLimits(10L, 2L, Duration.ofSeconds(2));
        final String email = "user@sitionix.com";

        //when
        this.loginUnauthorized(email);
        this.loginUnauthorized(email);
        this.loginTooMany(email, new AtomicReference<>());
        this.mutableClock.advance(Duration.ofSeconds(3));

        //then
        this.loginUnauthorized(email);
    }

    @Test
    @DisplayName("Should enforce IP limit even with different emails")
    void givenDifferentEmailsSameIp_whenLoginTooManyTimes_thenTooManyRequests() {
        //given
        this.setupActiveUser();
        this.configureLoginLimits(2L, 100L, Duration.ofSeconds(5));
        final AtomicReference<String> retryAfter = new AtomicReference<>();

        //when
        this.loginUnauthorized("user-1@sitionix.com");
        this.loginUnauthorized("user-2@sitionix.com");
        this.loginTooMany("user-3@sitionix.com", retryAfter);

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

    private void configureLoginLimits(final long ipLimit, final long emailLimit, final Duration window) {
        this.rateLimitProperties.setEnabled(true);
        final RateLimitProperties.EndpointLimits login = this.rateLimitProperties.getLogin();

        login.getIp().setEnabled(true);
        login.getIp().setLimit(ipLimit);
        login.getIp().setWindow(window);

        login.getEmail().setEnabled(true);
        login.getEmail().setLimit(emailLimit);
        login.getEmail().setWindow(window);
    }

    private void loginUnauthorized(final String email) {
        this.usedEmails.add(email);
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginUnauthorized())
                .withRequest("loginRequest.json", request -> {
                    request.setEmail(email);
                    request.setPassword("wrong-password");
                })
                .expectStatus(HttpStatus.UNAUTHORIZED)
                .andExpectPath(result -> {
                    if (this.clientIp == null) {
                        this.clientIp = result.getRequest().getRemoteAddr();
                    }
                })
                .assertAndCreate();
    }

    private void loginTooMany(final String email, final AtomicReference<String> retryAfter) {
        this.usedEmails.add(email);
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginUnauthorized())
                .withRequest("loginRequest.json", request -> {
                    request.setEmail(email);
                    request.setPassword("wrong-password");
                })
                .expectStatus(HttpStatus.TOO_MANY_REQUESTS)
                .andExpectPath(MockMvcResultMatchers.header().exists(HttpHeaders.RETRY_AFTER))
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.code")
                        .value(HttpStatus.TOO_MANY_REQUESTS.value()))
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.title")
                        .value(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase()))
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.details",
                        Matchers.containsString("Too many requests")))
                .andExpectPath(result -> {
                    if (this.clientIp == null) {
                        this.clientIp = result.getRequest().getRemoteAddr();
                    }
                    retryAfter.set(result.getResponse().getHeader(HttpHeaders.RETRY_AFTER));
                })
                .assertAndCreate();
    }

    @TestConfiguration
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.parse("2099-01-01T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    static class MutableClock extends Clock {

        private final ZoneId zone;
        private final AtomicReference<Instant> instant;

        MutableClock(final Instant initialInstant, final ZoneId zone) {
            this.zone = zone;
            this.instant = new AtomicReference<>(initialInstant);
        }

        @Override
        public ZoneId getZone() {
            return this.zone;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return new MutableClock(this.instant.get(), zone);
        }

        @Override
        public Instant instant() {
            return this.instant.get();
        }

        void advance(final Duration duration) {
            this.instant.updateAndGet(current -> current.plus(duration));
        }
    }
}
