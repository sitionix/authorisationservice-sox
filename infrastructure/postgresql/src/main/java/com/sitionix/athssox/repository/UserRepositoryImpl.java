package com.sitionix.athssox.repository;

import com.sitionix.athssox.exception.EmailAlreadyRegisteredException;
import com.sitionix.athssox.domain.RegisterUserDO;
import com.sitionix.athssox.domain.ResponseRegisterUser;
import com.sitionix.athssox.entity.UserEntity;
import com.sitionix.athssox.jpa.UserJpaRepository;
import com.sitionix.athssox.mapper.UserInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    private final UserInfraMapper userInfraMapper;

    @Override
    public ResponseRegisterUser createUser(final RegisterUserDO registerUserDO) {

        try {
            final UserEntity userEntity = this.userInfraMapper.asUserEntity(registerUserDO);
            final UserEntity createdUser = this.userJpaRepository.save(userEntity);

            return this.userInfraMapper.asResponseRegisterUser(createdUser);
        } catch (final DataIntegrityViolationException exception) {
            if (isEmailUniquenessViolation(exception)) {
                throw new EmailAlreadyRegisteredException(
                        "Email already registered for this role and context",
                        exception
                );
            }
            throw exception;
        }
    }

    private static boolean isEmailUniquenessViolation(final DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintViolationException) {
                final String constraintName = constraintViolationException.getConstraintName();
                return constraintName != null && constraintName.startsWith("uq_users_email");
            }
            cause = cause.getCause();
        }
        return false;
    }
}
