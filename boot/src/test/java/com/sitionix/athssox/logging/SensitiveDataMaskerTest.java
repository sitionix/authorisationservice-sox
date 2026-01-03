package com.sitionix.athssox.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SensitiveDataMaskerTest {

    @Mock
    private Object dependency;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.dependency);
    }

    @Test
    void givenJsonWithTo_whenMask_thenHideEmail() {
        //given
        final String given = this.getPayloadWithTo();
        final String expected = this.getPayloadWithMaskedTo();

        //when
        final String actual = SensitiveDataMasker.mask(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenJsonWithWhitespaceAndMultipleTo_whenMask_thenHideAllEmails() {
        //given
        final String given = this.getPayloadWithMultipleTo();
        final String expected = this.getPayloadWithMultipleMaskedTo();

        //when
        final String actual = SensitiveDataMasker.mask(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenMessageWithoutTo_whenMask_thenReturnOriginal() {
        //given
        final String given = this.getMessageWithoutTo();

        //when
        final String actual = SensitiveDataMasker.mask(given);

        //then
        assertThat(actual).isEqualTo(given);
    }

    @Test
    void givenNullMessage_whenMask_thenReturnNull() {
        //given
        final String given = this.getNullMessage();
        final String expected = null;

        //when
        final String actual = SensitiveDataMasker.mask(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenBlankMessage_whenMask_thenReturnBlank() {
        //given
        final String given = this.getBlankMessage();

        //when
        final String actual = SensitiveDataMasker.mask(given);

        //then
        assertThat(actual).isEqualTo(given);
    }

    @Test
    void givenJsonWithToken_whenMask_thenHideToken() {
        //given
        final String given = this.getPayloadWithToken();
        final String expected = this.getPayloadWithMaskedToken();

        //when
        final String actual = SensitiveDataMasker.mask(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenJsonWithVerifyUrl_whenMask_thenHideTokenInUrl() {
        //given
        final String given = this.getPayloadWithVerifyUrl();
        final String expected = this.getPayloadWithMaskedVerifyUrl();

        //when
        final String actual = SensitiveDataMasker.mask(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenMessageWithTokenQueryParam_whenMask_thenHideTokenValue() {
        //given
        final String given = this.getMessageWithTokenQueryParam();
        final String expected = this.getMessageWithMaskedTokenQueryParam();

        //when
        final String actual = SensitiveDataMasker.mask(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private String getPayloadWithTo() {
        return "{\"delivery\":{\"to\":\"email@sitionix.com\"}}";
    }

    private String getPayloadWithMaskedTo() {
        return "{\"delivery\":{\"to\":\"e***@sitionix.com\"}}";
    }

    private String getPayloadWithMultipleTo() {
        return "{\"delivery\": { \"to\" : \"first@sitionix.com\" }, \"alt\": {\"to\":\"second@sitionix.com\"}}";
    }

    private String getPayloadWithMultipleMaskedTo() {
        return "{\"delivery\": { \"to\":\"f***@sitionix.com\" }, \"alt\": {\"to\":\"s***@sitionix.com\"}}";
    }

    private String getMessageWithoutTo() {
        return "{\"delivery\":{\"channel\":\"EMAIL\"}}";
    }

    private String getNullMessage() {
        return null;
    }

    private String getBlankMessage() {
        return " ";
    }

    private String getPayloadWithToken() {
        return "{\"token\":\"secret-token\"}";
    }

    private String getPayloadWithMaskedToken() {
        return "{\"token\":\"***\"}";
    }

    private String getPayloadWithVerifyUrl() {
        return "{\"params\":{\"verifyUrl\":\"https://frontend.sitionix.com/auth/email/verify?token=secret-token&siteId=site-id\"}}";
    }

    private String getPayloadWithMaskedVerifyUrl() {
        return "{\"params\":{\"verifyUrl\":\"https://frontend.sitionix.com/auth/email/verify?token=***&siteId=site-id\"}}";
    }

    private String getMessageWithTokenQueryParam() {
        return "https://frontend.sitionix.com/auth/email/verify?token=secret-token";
    }

    private String getMessageWithMaskedTokenQueryParam() {
        return "https://frontend.sitionix.com/auth/email/verify?token=***";
    }
}
