package com.sitionix.athssox.repository;

import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.entity.UserEntity;
import com.sitionix.athssox.jpa.UserJpaRepository;
import com.sitionix.athssox.mapper.UserInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
