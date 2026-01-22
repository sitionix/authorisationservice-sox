package com.sitionix.athssox.it;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class EmailVerificationTokenIssueLinkIT {

    private static final UUID VALID_TOKEN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USED_TOKEN_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID EXPIRED_TOKEN_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID VALID_PEPPER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USED_PEPPER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EXPIRED_PEPPER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SITE_ID = UUID.fromString("c9b1f3f4-12c7-11ec-82a8-0242ac130003");
    private static final String EXPECTED_EXPIRES_AT = "2099-01-02T12:00:00Z";

    @Autowired
    private TestManager testManager;

    @Value("${bff.base-url}")
    private String bffBaseUrl;

    @Value("${security.email-verification.hmac-secret}")
    private String hmacSecret;

    @Test
    @DisplayName("Should issue verification link for active token")
    void givenActiveToken_whenIssueLink_thenReturnVerifyUrlAndMetadata() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserPendingEmailVerifyEntity.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenIssueLinkValid.json"))
                .build();

        final List<String> verifyUrls = new ArrayList<>();
        final List<String> expiresAt = new ArrayList<>();
        final List<String> tokenIds = new ArrayList<>();

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.issueEmailVerificationLink())
                .withPathParameters(PathParams.create()
                        .add("tokenId", VALID_TOKEN_ID))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", VALID_PEPPER_ID))
                .expectStatus(HttpStatus.OK)
                .andExpectPath(result -> verifyUrls.add(JsonPath.read(result.getResponse().getContentAsString(), "$.verifyUrl")))
                .andExpectPath(result -> expiresAt.add(JsonPath.read(result.getResponse().getContentAsString(), "$.expiresAt")))
                .andExpectPath(result -> tokenIds.add(JsonPath.read(result.getResponse().getContentAsString(), "$.tokenId")))
                .andExpectPath(MockMvcResultMatchers.jsonPath("$.token").doesNotExist())
                .assertAndCreate();

        //then
        assertThat(verifyUrls).hasSize(1);
        assertThat(expiresAt).hasSize(1);
        assertThat(tokenIds).containsExactly(VALID_TOKEN_ID.toString());

        final String verifyUrl = verifyUrls.get(0);
        final UriComponents components = UriComponentsBuilder.fromUriString(verifyUrl).build();
        final EmailVerificationSecurityConfig config = new EmailVerificationSecurityConfig();
        config.setHmacSecret(this.hmacSecret);
        final HmacEmailVerificationTokenSigner signer = new HmacEmailVerificationTokenSigner(config);
        final String expectedToken = signer.buildToken(VALID_TOKEN_ID, VALID_PEPPER_ID);

        assertThat(verifyUrl).startsWith(this.bffBaseUrl + "/api/v1/auth/email/verify");
        assertThat(components.getQueryParams().getFirst("token")).isEqualTo(expectedToken);
        assertThat(components.getQueryParams().getFirst("siteId")).isEqualTo(SITE_ID.toString());
        assertThat(OffsetDateTime.parse(expiresAt.get(0))).isEqualTo(OffsetDateTime.parse(EXPECTED_EXPIRES_AT));
    }

    @Test
    @DisplayName("Should return 404 when token does not exist")
    void givenMissingToken_whenIssueLink_thenNotFound() {
        //given
        final UUID tokenId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.issueEmailVerificationLink())
                .withPathParameters(PathParams.create()
                        .add("tokenId", tokenId))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", VALID_PEPPER_ID))
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
                        .add("tokenId", EXPIRED_TOKEN_ID))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", EXPIRED_PEPPER_ID))
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
                        .add("tokenId", USED_TOKEN_ID))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", USED_PEPPER_ID))
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
                        .add("tokenId", VALID_TOKEN_ID))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", VALID_PEPPER_ID))
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
        final String tokenId = "not-a-uuid";

        //when
        this.testManager.mockMvc()
                .ping(ControllerEndpoint.issueEmailVerificationLink())
                .withPathParameters(PathParams.create()
                        .add("tokenId", tokenId))
                .withQueryParameters(QueryParams.create()
                        .add("pepper", VALID_PEPPER_ID))
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
        final String token = signer.buildToken(VALID_TOKEN_ID, VALID_PEPPER_ID);
        final boolean containsSensitive;
        try {
            //when
            this.testManager.mockMvc()
                    .ping(ControllerEndpoint.issueEmailVerificationLink())
                    .withPathParameters(PathParams.create()
                            .add("tokenId", VALID_TOKEN_ID))
                    .withQueryParameters(QueryParams.create()
                            .add("pepper", VALID_PEPPER_ID))
                    .expectStatus(HttpStatus.OK)
                    .assertAndCreate();

            containsSensitive = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .anyMatch(message -> message.contains("token=")
                            || message.contains(token)
                            || message.contains(VALID_PEPPER_ID.toString()));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        //then
        assertThat(containsSensitive).isFalse();
    }
}
