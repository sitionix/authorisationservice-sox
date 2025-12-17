package com.sitionix.athssox.domain.usecase;

import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.domain.model.RegisterUserDO;

public interface RegisterUser {

    ResponseRegisterUser execute(final RegisterUserDO registerUserDO);
}
