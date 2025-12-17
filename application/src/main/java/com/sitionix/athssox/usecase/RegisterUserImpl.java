package com.sitionix.athssox.usecase;

import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.domain.UserStatus;
import com.sitionix.athssox.repository.UserRepository;
import com.sitionix.athssox.validator.PasswordPolicyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class RegisterUserImpl implements RegisterUser {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    @Override
    public ResponseRegisterUser execute(final RegisterUserDO registerUserDO) {
        this.passwordPolicyValidator.validate(registerUserDO.getPassword());

        final RegisterUserDO userToCreate = RegisterUserDO.builder()
                .email(registerUserDO.getEmail())
                .password(this.passwordEncoder.encode(registerUserDO.getPassword()))
                .role(registerUserDO.getRole())
                .siteId(registerUserDO.getSiteId())
                .status(UserStatus.PENDING_EMAIL_VERIFY)
                .build();

        final ResponseRegisterUser createdUser = this.userRepository.createUser(userToCreate);
        createdUser.setMessage("Registration successful. Please verify your email.");
        return createdUser;
    }
}
