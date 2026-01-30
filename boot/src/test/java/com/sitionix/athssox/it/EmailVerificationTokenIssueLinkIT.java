package com.sitionix.athssox.it;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sitionix.athssox.api.controller.AuthController;
import com.sitionix.athssox.application.config.EmailVerificationSecurityConfig;
import com.sitionix.athssox.application.service.HmacEmailVerificationTokenSigner;
import com.sitionix.athssox.it.infra.ControllerEndpoint;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import com.sitionix.forgeit.mockmvc.api.PathParams;
import com.sitionix.forgeit.mockmvc.api.QueryParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class EmailVerificationTokenIssueLinkIT {

    @Autowired
    private TestManager testManager;

    @Value("${security.email-verification.hmac-secret}")
    private String hmacSecret;

    @Test
    @DisplayName("Should issue verification link for active token")
    void givenActiveToken_whenIssueLink_thenReturnResponse() {
        //given
        final UUID tokenId = this.getValidTokenId();
        final UUID pepperId = this.getValidPepperId();

        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserPendingEmailVerifyEntity.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenIssueLinkValid.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.issueEmailVerificationLink())
                .withPathParameters(PathParams.create()
                        .add("tokenId", tokenId))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", pepperId))
                .expectStatus(HttpStatus.OK)
                .expectResponse("issueEmailVerificationLinkResponse.json")
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should return 404 when token does not exist")
    void givenMissingToken_whenIssueLink_thenNotFound() {
        //given
        final UUID tokenId = this.getMissingTokenId();
        final UUID pepperId = this.getValidPepperId();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.issueEmailVerificationLink())
                .withPathParameters(PathParams.create()
                        .add("tokenId", tokenId))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", pepperId))
                .expectStatus(HttpStatus.NOT_FOUND)
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()))
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.title").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should return 410 when token is expired")
    void givenExpiredToken_whenIssueLink_thenGone() {
        //given
        final UUID tokenId = this.getExpiredTokenId();
        final UUID pepperId = this.getExpiredPepperId();

        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserPendingEmailVerifyEntity.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenIssueLinkExpired.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.issueEmailVerificationLink())
                .withPathParameters(PathParams.create()
                        .add("tokenId", tokenId))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", pepperId))
                .expectStatus(HttpStatus.GONE)
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.code").value(HttpStatus.GONE.value()))
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.title").value(HttpStatus.GONE.getReasonPhrase()))
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should return 410 when token is used")
    void givenUsedToken_whenIssueLink_thenGone() {
        //given
        final UUID tokenId = this.getUsedTokenId();
        final UUID pepperId = this.getUsedPepperId();

        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserPendingEmailVerifyEntity.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenIssueLinkUsed.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.issueEmailVerificationLink())
                .withPathParameters(PathParams.create()
                        .add("tokenId", tokenId))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", pepperId))
                .expectStatus(HttpStatus.GONE)
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.code").value(HttpStatus.GONE.value()))
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.title").value(HttpStatus.GONE.getReasonPhrase()))
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should return 409 when user is already verified")
    void givenActiveUser_whenIssueLink_thenConflict() {
        //given
        final UUID tokenId = this.getValidTokenId();
        final UUID pepperId = this.getValidPepperId();

        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenIssueLinkValid.json"))
                .build();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.issueEmailVerificationLink())
                .withPathParameters(PathParams.create()
                        .add("tokenId", tokenId))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", pepperId))
                .expectStatus(HttpStatus.CONFLICT)
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.code").value(HttpStatus.CONFLICT.value()))
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.title").value(HttpStatus.CONFLICT.getReasonPhrase()))
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should return 400 when token id format is invalid")
    void givenInvalidTokenId_whenIssueLink_thenBadRequest() {
        //given
        final String tokenId = this.getInvalidTokenId();
        final UUID pepperId = this.getValidPepperId();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.issueEmailVerificationLink())
                .withPathParameters(PathParams.create()
                        .add("tokenId", tokenId))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", pepperId))
                .expectStatus(HttpStatus.BAD_REQUEST)
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.title").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .assertAndCreate();

        //then
    }

    @Test
    @DisplayName("Should not log verification token data on issue link")
    void givenIssueLink_whenLogged_thenTokenNotPresentInLogs() {
        //given
        final UUID tokenId = this.getValidTokenId();
        final UUID pepperId = this.getValidPepperId();

        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserPendingEmailVerifyEntity.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenIssueLinkValid.json"))
                .build();

        final Logger logger = (Logger) LoggerFactory.getLogger(AuthController.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        final EmailVerificationSecurityConfig config = new EmailVerificationSecurityConfig();
        config.setHmacSecret(this.hmacSecret);
        final HmacEmailVerificationTokenSigner signer = new HmacEmailVerificationTokenSigner(config);
        final String token = signer.buildToken(tokenId, pepperId);
        final boolean containsSensitive;
        try {
            //when
            this.testManager.mockMvc()
                    .ping(ControllerEndpoint.issueEmailVerificationLink())
                    .withPathParameters(PathParams.create()
                            .add("tokenId", tokenId))
                    .withQueryParameters(QueryParams.create()
                            .add("pepper", pepperId))
                    .expectStatus(HttpStatus.OK)
                    .assertAndCreate();

            containsSensitive = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("token=")
                            || message.contains(token)
                            || message.contains(pepperId.toString()));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        //then
        assertThat(containsSensitive).isFalse();
    }

    private UUID getValidTokenId() {
        return UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }

    private UUID getUsedTokenId() {
        return UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    }

    private UUID getExpiredTokenId() {
        return UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    }

    private UUID getMissingTokenId() {
        return UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }

    private UUID getValidPepperId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    private UUID getUsedPepperId() {
        return UUID.fromString("22222222-2222-2222-2222-222222222222");
    }

    private UUID getExpiredPepperId() {
        return UUID.fromString("33333333-3333-3333-3333-333333333333");
    }

    private String getInvalidTokenId() {
        return "not-a-uuid";
    }
}
