package com.sitionix.athssox.pipe.producer.mapper;

import com.app_afesox.ntfssox.events.notifications.contents.EmailVerificationContentDTO;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailVerifyContentEventMapperTest {

    private EmailVerifyContentEventMapper emailVerifyContentEventMapper;

    @BeforeEach
    void setUp() {
        this.emailVerifyContentEventMapper = new EmailVerifyContentEventMapperImpl();
    }

    @Test
    void givenParams_whenAsContent_thenReturnContent() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();
        final EmailVerifyPayload.Params given = this.getParams(tokenId, pepperId);
        final EmailVerificationContentDTO expected = this.getContent(tokenId.toString(), pepperId.toString());

        //when
        final EmailVerificationContentDTO actual = this.emailVerifyContentEventMapper.asContent(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private EmailVerifyPayload.Params getParams(final UUID tokenId,
                                                final UUID pepperId) {
        return EmailVerifyPayload.Params.builder()
                .emailVerificationTokenId(tokenId)
                .pepperId(pepperId)
                .build();
    }

    private EmailVerificationContentDTO getContent(final String tokenId,
                                                   final String pepperId) {
        return EmailVerificationContentDTO.newBuilder()
                .setVerificationTokenId(tokenId)
                .setPepperId(pepperId)
                .build();
    }

    private UUID getTokenId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    private UUID getPepperId() {
        return UUID.fromString("22222222-2222-2222-2222-222222222222");
    }
}
