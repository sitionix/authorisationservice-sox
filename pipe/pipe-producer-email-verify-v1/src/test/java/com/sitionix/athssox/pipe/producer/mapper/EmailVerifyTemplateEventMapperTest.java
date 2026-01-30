package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.ntfssox.events.notifications.NotificationTemplateDTO;
import com.sitionix.athssox.domain.model.outbox.payload.NotificationTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailVerifyTemplateEventMapperTest {

    private EmailVerifyTemplateEventMapper emailVerifyTemplateEventMapper;

    @BeforeEach
    void setUp() {
        this.emailVerifyTemplateEventMapper = new EmailVerifyTemplateEventMapperImpl();
    }

    @Test
    void givenTemplate_whenAsTemplate_thenReturnTemplateDto() {
        //given
        final NotificationTemplate given = NotificationTemplate.EMAIL_VERIFY;
        final NotificationTemplateDTO expected = NotificationTemplateDTO.EMAIL_VERIFY;

        //when
        final NotificationTemplateDTO actual = this.emailVerifyTemplateEventMapper.asTemplate(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }
}
