package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.EmailVerificationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailVerifyApiMapperTest {

    private EmailVerifyApiMapper emailVerifyApiMapper;

    @BeforeEach
    void setUp() {
        this.emailVerifyApiMapper = new EmailVerifyApiMapperImpl();
    }

    @Test
    void given_verified_true_when_as_email_verification_response_dto_then_return_active_response() {
        //given
        final boolean verified = true;
        final EmailVerificationResponseDTO expected = this.getEmailVerificationResponseDTO("Email verified successfully.",
                EmailVerificationResponseDTO.StatusEnum.ACTIVE);

        //when
        final EmailVerificationResponseDTO actual = this.emailVerifyApiMapper.asEmailVerificationResponseDTO(verified);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_verified_false_when_as_email_verification_response_dto_then_return_accepted_response() {
        //given
        final boolean verified = false;
        final EmailVerificationResponseDTO expected = this.getEmailVerificationResponseDTO("Email verification accepted.",
                null);

        //when
        final EmailVerificationResponseDTO actual = this.emailVerifyApiMapper.asEmailVerificationResponseDTO(verified);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private EmailVerificationResponseDTO getEmailVerificationResponseDTO(final String message,
                                                                         final EmailVerificationResponseDTO.StatusEnum status) {
        return EmailVerificationResponseDTO.builder()
                .message(message)
                .status(status)
                .build();
    }
}
