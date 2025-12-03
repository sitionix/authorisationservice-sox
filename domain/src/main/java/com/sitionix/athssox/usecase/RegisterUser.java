package com.sitionix.athssox.usecase;

import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;

public interface RegisterUser {

    ResponseRegisterUser execute(final RegisterUserDO registerUserDO);
}
