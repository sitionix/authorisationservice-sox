package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.RegisterUserDTO;
import com.app_afesox.athssox.api_first.dto.ResponseRegisterUserDTO;
import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserApiMapperTest {

    private UserApiMapper userApiMapper;

    @BeforeEach
    void setUp() {
        this.userApiMapper = new UserApiMapperImpl();
    }

    @Test
    void given_user_when_as_user_response_dto_then_return_user_response_dto() {
        //given

        final UUID siteId = UUID.randomUUID();

        final RegisterUserDO expected = this.getRegisterUser(siteId);
        final RegisterUserDTO given = this.getRegisterUserDTO(siteId);

        //when
        final RegisterUserDO actual = this.userApiMapper.asRegisterUser(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_user_when_as_response_register_user_dto_then_return_response_register_user_dto() {
        //given
        final Long userId = 1L;

        final ResponseRegisterUserDTO expected = this.getResponseRegisterUserDTO();
        final ResponseRegisterUser given = this.getResponseRegisterUser(userId);

        //when
        final ResponseRegisterUserDTO actual = this.userApiMapper.asResponseRegisterUserDTO(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private ResponseRegisterUserDTO getResponseRegisterUserDTO() {
        return ResponseRegisterUserDTO.builder()
                .message("User registered successfully")
                .status(ResponseRegisterUserDTO.StatusEnum.PENDING_EMAIL_VERIFY)
                .userId(1L)
                .build();
    }

    private RegisterUserDTO getRegisterUserDTO(final UUID siteId) {
        return RegisterUserDTO.builder()
                .password("password")
                .email("email@sitionix.com")
                .role(RegisterUserDTO.RoleEnum.SITE_USER)
                .siteId(siteId)
                .build();
    }

    private ResponseRegisterUser getResponseRegisterUser(final Long userId) {
        return ResponseRegisterUser.builder()
                .userId(userId)
                .status(UserStatus.PENDING_EMAIL_VERIFY)
                .message("User registered successfully")
                .build();
    }

    private RegisterUserDO getRegisterUser(final UUID siteId) {
        return RegisterUserDO.builder()
                .email("email@sitionix.com")
                .role(UserRole.SITE_USER)
                .status(UserStatus.PENDING_EMAIL_VERIFY)
                .siteId(siteId)
                .password("password")
                .build();
    }

}
