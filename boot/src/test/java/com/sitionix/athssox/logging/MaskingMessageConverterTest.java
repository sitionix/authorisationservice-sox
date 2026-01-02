package com.sitionix.athssox.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaskingMessageConverterTest {

    private MaskingMessageConverter maskingMessageConverter;

    @Mock
    private ILoggingEvent loggingEvent;

    @BeforeEach
    void setUp() {
        this.maskingMessageConverter = new MaskingMessageConverter();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.loggingEvent);
    }

    @Test
    void givenMessageWithEmail_whenConvert_thenMaskEmail() {
        //given
        final String given = this.getMessageWithTo();
        final String expected = this.getMessageWithMaskedTo();

        when(this.loggingEvent.getFormattedMessage())
                .thenReturn(given);

        //when
        final String actual = this.maskingMessageConverter.convert(this.loggingEvent);

        //then
        assertThat(actual).isEqualTo(expected);
        verify(this.loggingEvent).getFormattedMessage();
    }

    @Test
    void givenMessageWithoutEmail_whenConvert_thenReturnOriginal() {
        //given
        final String given = this.getMessageWithoutTo();

        when(this.loggingEvent.getFormattedMessage())
                .thenReturn(given);

        //when
        final String actual = this.maskingMessageConverter.convert(this.loggingEvent);

        //then
        assertThat(actual).isEqualTo(given);
        verify(this.loggingEvent).getFormattedMessage();
    }

    private String getMessageWithTo() {
        return "{\"delivery\":{\"to\":\"email@sitionix.com\"}}";
    }

    private String getMessageWithMaskedTo() {
        return "{\"delivery\":{\"to\":\"e***@sitionix.com\"}}";
    }

    private String getMessageWithoutTo() {
        return "{\"delivery\":{\"channel\":\"EMAIL\"}}";
    }
}
