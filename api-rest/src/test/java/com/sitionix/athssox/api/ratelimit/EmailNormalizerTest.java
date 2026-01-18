package com.sitionix.athssox.api.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class EmailNormalizerTest {

    private EmailNormalizer emailNormalizer;

    @Mock
    private Object dependency;

    @BeforeEach
    void setUp() {
        this.emailNormalizer = new EmailNormalizer();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.dependency);
    }

    @Test
    void givenEmailWithWhitespace_whenNormalize_thenReturnLowercaseTrimmed() {
        //given
        final String given = this.getEmailWithWhitespace();
        final String expected = this.getNormalizedEmail();

        //when
        final String actual = this.emailNormalizer.normalize(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenBlankEmail_whenNormalize_thenReturnNull() {
        //given
        final String given = this.getBlankEmail();

        //when
        final String actual = this.emailNormalizer.normalize(given);

        //then
        assertThat(actual).isNull();
    }

    private String getEmailWithWhitespace() {
        return "  USER@SITIONIX.COM ";
    }

    private String getNormalizedEmail() {
        return "user@sitionix.com";
    }

    private String getBlankEmail() {
        return "   ";
    }
}
