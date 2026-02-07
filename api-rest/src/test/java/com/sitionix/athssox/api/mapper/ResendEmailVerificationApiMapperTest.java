package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.ResendEmailVerificationResponseDTO;
import com.sitionix.athssox.domain.model.ResendEmailVerificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ResendEmailVerificationApiMapperTest {

    private ResendEmailVerificationApiMapper mapper;

    @BeforeEach
    void setUp() {
        this.mapper = new ResendEmailVerificationApiMapperImpl();
    }

    @Test
    void givenResendEmailVerificationResponse_whenAsResendEmailVerificationResponseDto_thenReturnResendEmailVerificationResponseDto() {
        //given
        final ResendEmailVerificationResponse given = this.resendEmailVerificationResponse();
        final ResendEmailVerificationResponseDTO expected = this.resendEmailVerificationResponseDTO();

        //when
        final ResendEmailVerificationResponseDTO actual =
                this.mapper.asResendEmailVerificationResponseDTO(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private ResendEmailVerificationResponse resendEmailVerificationResponse() {
        return ResendEmailVerificationResponse.builder()
                .message("message")
                .build();
    }

    private ResendEmailVerificationResponseDTO resendEmailVerificationResponseDTO() {
        return ResendEmailVerificationResponseDTO.builder()
                .message("message")
                .build();
    }
}
