package com.sitionix.athssox.postgresql.repository.user;

import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.repository.UserRepository;
import com.sitionix.athssox.postgresql.entity.user.UserEntity;
import com.sitionix.athssox.postgresql.jpa.user.UserJpaRepository;
import com.sitionix.athssox.postgresql.mapper.user.UserInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    private final UserInfraMapper userInfraMapper;

    @Override
    public ResponseRegisterUser createUser(final RegisterUserDO registerUserDO) {

        final UserEntity userEntity = this.userInfraMapper.asUserEntity(registerUserDO);
        final UserEntity createdUser = this.userJpaRepository.save(userEntity);

        return this.userInfraMapper.asResponseRegisterUser(createdUser);
    }

    @Override
    public boolean existsSiteScopedByEmailAndSiteId(final String email, final UUID siteId) {
        return this.userJpaRepository.existsByEmailAndSiteIdAndGlobalRole_IdIn(email,
                siteId,
                UserRole.siteScopedIds());
    }

    @Override
    public boolean existsGlobalByEmail(final String email) {
        return this.userJpaRepository.existsByEmailAndGlobalRole_IdIn(email,
                UserRole.globalScopedIds());
    }

}
