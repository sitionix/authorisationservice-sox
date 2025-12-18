package com.sitionix.athssox.domain.repository;

import com.sitionix.athssox.domain.model.AuthUser;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository {

    Optional<AuthUser> findByEmailAndSiteId(final String email, final UUID siteId);

    Optional<AuthUser> findGlobalByEmail(final String email);
}
