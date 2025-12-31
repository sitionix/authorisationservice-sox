package com.sitionix.athssox.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
    }

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
}
