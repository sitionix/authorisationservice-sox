package com.sitionix.athssox.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DefaultVerificationLinkFactoryTest {

    private DefaultVerificationLinkFactory defaultVerificationLinkFactory;

    @BeforeEach
    void setUp() {
        this.defaultVerificationLinkFactory = new DefaultVerificationLinkFactory();
        this.setBaseUrl(this.defaultVerificationLinkFactory, this.getBaseUrl());
    }

    @Test
    void given_token_and_null_site_id_when_build_email_verify_url_then_return_url_with_token_only() {
        //given
        final String rawToken = this.getRawToken();
        final String expected = this.getExpectedUrlWithoutSiteId(rawToken);

        //when
        final String actual = this.defaultVerificationLinkFactory.buildEmailVerifyUrl(rawToken, null);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_token_and_site_id_when_build_email_verify_url_then_return_url_with_token_and_site_id() {
        //given
        final String rawToken = this.getRawToken();
        final UUID siteId = this.getSiteId();
        final String expected = this.getExpectedUrlWithSiteId(rawToken, siteId);

        //when
        final String actual = this.defaultVerificationLinkFactory.buildEmailVerifyUrl(rawToken, siteId);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private void setBaseUrl(final DefaultVerificationLinkFactory factory, final String baseUrl) {
        ReflectionTestUtils.setField(factory, "baseUrl", baseUrl);
    }

    private String getBaseUrl() {
        return "https://bff.sitionix.com";
    }

    private String getRawToken() {
        return "token+value";
    }

    private UUID getSiteId() {
        return UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    }

    private String getExpectedUrlWithoutSiteId(final String rawToken) {
        return this.getBaseUrl() + "/api/v1/auth/email/verify?token=" + this.getEncoded(rawToken);
    }

    private String getExpectedUrlWithSiteId(final String rawToken, final UUID siteId) {
        return this.getBaseUrl() + "/api/v1/auth/email/verify?token=" + this.getEncoded(rawToken)
                + "&siteId=" + this.getEncoded(siteId.toString());
    }

    private String getEncoded(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
