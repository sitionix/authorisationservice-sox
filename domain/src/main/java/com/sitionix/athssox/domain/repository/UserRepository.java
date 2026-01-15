package com.sitionix.athssox.domain.repository;

import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;

import java.util.UUID;
import java.util.Optional;

public interface UserRepository {

    ResponseRegisterUser createUser(final RegisterUserDO registerUserDO);

    boolean existsSiteScopedByEmailAndSiteId(final String email, final UUID siteId);

    boolean existsGlobalByEmail(final String email);

    Optional<ResponseRegisterUser> findSiteScopedByEmailAndSiteId(final String email, final UUID siteId);

    Optional<ResponseRegisterUser> findGlobalByEmail(final String email);
}
