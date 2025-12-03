package com.sitionix.athssox.usecase;

import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterUserImpl implements RegisterUser {

    private final UserRepository userRepository;

    @Override
    public ResponseRegisterUser execute(final RegisterUserDO registerUserDO) {
        return this.userRepository.createUser(registerUserDO);
    }
}
