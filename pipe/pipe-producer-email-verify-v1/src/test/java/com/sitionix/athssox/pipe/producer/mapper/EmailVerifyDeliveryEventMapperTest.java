package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.ntfssox.events.notifications.DeliveryDTO;
import com.app_afesox.ntfssox.events.notifications.NotificationChannelDTO;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.VerifyChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailVerifyDeliveryEventMapperTest {

    private EmailVerifyDeliveryEventMapper emailVerifyDeliveryEventMapper;

    @BeforeEach
    void setUp() {
        this.emailVerifyDeliveryEventMapper = new EmailVerifyDeliveryEventMapperImpl();
    }

    @Test
    void givenDelivery_whenAsDelivery_thenReturnDeliveryDto() {
        //given
        final EmailVerifyPayload.Delivery given = this.getDelivery();
        final DeliveryDTO expected = this.getDeliveryDto();

        //when
        final DeliveryDTO actual = this.emailVerifyDeliveryEventMapper.asDelivery(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenChannel_whenAsChannel_thenReturnNotificationChannelDto() {
        //given
        final VerifyChannel given = VerifyChannel.EMAIL;
        final NotificationChannelDTO expected = NotificationChannelDTO.EMAIL;

        //when
        final NotificationChannelDTO actual = this.emailVerifyDeliveryEventMapper.asChannel(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private EmailVerifyPayload.Delivery getDelivery() {
        return EmailVerifyPayload.Delivery.builder()
                .channel(VerifyChannel.EMAIL)
                .to("user@sitionix.com")
                .build();
    }

    private DeliveryDTO getDeliveryDto() {
        return DeliveryDTO.newBuilder()
                .setChannel(NotificationChannelDTO.EMAIL)
                .setTo("user@sitionix.com")
                .build();
    }
}
