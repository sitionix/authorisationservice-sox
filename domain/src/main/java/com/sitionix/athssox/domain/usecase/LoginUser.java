package com.sitionix.athssox.domain.usecase;

import com.sitionix.athssox.domain.model.LoginRequest;
import com.sitionix.athssox.domain.model.LoginResponse;

public interface LoginUser {

    LoginResponse execute(final LoginRequest loginRequest);
}
