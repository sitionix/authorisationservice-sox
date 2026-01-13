package com.sitionix.athssox.postgresql.mapper.token;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenEntity;
import com.sitionix.athssox.postgresql.entity.user.UserEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {EmailVerificationTokenStatusInfraMapper.class})
public interface EmailVerificationTokenInfraMapper {

    @Mapping(target = "user", source = "userId")
    @Mapping(target = "createdAt", ignore = true)
    EmailVerificationTokenEntity asEntity(final EmailVerificationTokenRecord tokenRecord);

    @Mapping(target = "userId", source = "user.id")
    EmailVerificationTokenRecord asRecord(EmailVerificationTokenEntity entity);

    default UserEntity map(final Long userId) {
        if (userId == null) {
            return null;
        }
        return UserEntity.builder()
                .id(userId)
                .build();
    }
}
