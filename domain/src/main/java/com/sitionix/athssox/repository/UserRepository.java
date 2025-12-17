package com.sitionix.athssox.repository;

import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;

import java.util.UUID;

public interface UserRepository {

    ResponseRegisterUser createUser(final RegisterUserDO registerUserDO);

    boolean existsSiteScopedByEmailAndSiteId(final String email, final UUID siteId);

    boolean existsGlobalByEmail(final String email);
}
