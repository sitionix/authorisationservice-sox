package com.sitionix.athssox.it;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.app_afesox.athssox.api_first.dto.LoginRequestDTO;
import com.sitionix.athssox.application.security.LoginAuthenticationProvider;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class AuthRequestLimitIT {

    @Autowired
    private TestManager testManager;

    @Test
    @DisplayName("Should reject oversized login request")
    void givenOversizedLoginRequest_whenLogin_thenPayloadTooLarge() {
        //given
        final String oversizedPassword = "A".repeat(700);

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.loginBadRequest())
                .withRequest("loginRequest.json", (LoginRequestDTO request) -> request.setPassword(oversizedPassword))
                .expectStatus(HttpStatus.PAYLOAD_TOO_LARGE)
                .expectResponse("requestTooLargeResponse.json")
                .assertAndCreate();
    }

    @Test
    @DisplayName("Should reject login when password exceeds max length without invoking password check")
    void givenTooLongPassword_whenLogin_thenBadRequestAndPasswordNotChecked() {
        //given
        final String longPassword = "A".repeat(129);
        final Logger logger = (Logger) LoggerFactory.getLogger(LoginAuthenticationProvider.class);
        final Level previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        final boolean passwordVerificationLogged;
        try {
            //when
            this.testManager.mockMvc()
                    .ping(ControllerEndpoint.loginBadRequest())
                    .withRequest("loginRequest.json", (LoginRequestDTO request) -> request.setPassword(longPassword))
                    .expectStatus(HttpStatus.BAD_REQUEST)
                    .expectResponse("requestInvalidResponse.json")
                    .assertAndCreate();

            passwordVerificationLogged = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("Password verification started for login."));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previousLevel);
        }

        //then
        assertThat(passwordVerificationLogged).isFalse();
    }
}
