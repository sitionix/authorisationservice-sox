package com.sitionix.athssox.repository;

import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;

public interface UserRepository {

    ResponseRegisterUser createUser(final RegisterUserDO registerUserDO);
}
