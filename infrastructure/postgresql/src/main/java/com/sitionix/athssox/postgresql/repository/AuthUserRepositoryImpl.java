package com.sitionix.athssox.postgresql.repository;

import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.repository.AuthUserRepository;
import com.sitionix.athssox.postgresql.jpa.UserJpaRepository;
import com.sitionix.athssox.postgresql.mapper.UserInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuthUserRepositoryImpl implements AuthUserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserInfraMapper userInfraMapper;

    @Override
    public Optional<AuthUser> findByEmailAndSiteId(final String email, final UUID siteId) {
        return this.userJpaRepository.findByEmailAndSiteId(email, siteId)
                .map(this.userInfraMapper::asAuthUser);
    }

    @Override
    public Optional<AuthUser> findGlobalByEmail(final String email) {
        return this.userJpaRepository.findByEmailAndSiteIdIsNull(email)
                .map(this.userInfraMapper::asAuthUser);
    }
}
