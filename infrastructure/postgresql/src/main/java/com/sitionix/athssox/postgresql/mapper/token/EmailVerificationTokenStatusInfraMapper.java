package com.sitionix.athssox.postgresql.mapper.token;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenStatusEntity;
import org.mapstruct.Mapper;

import static java.util.Objects.isNull;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface EmailVerificationTokenStatusInfraMapper {

    default EmailVerificationTokenStatus asStatus(final EmailVerificationTokenStatusEntity statusEntity) {
        if (isNull(statusEntity)) {
            return null;
        }
        return EmailVerificationTokenStatus.fromId(statusEntity.getId());
    }

    default EmailVerificationTokenStatusEntity asStatusEntity(final EmailVerificationTokenStatus status) {
        if (isNull(status)) {
            return null;
        }
        return EmailVerificationTokenStatusEntity.builder()
                .id(status.getId())
                .description(status.getDescription())
                .build();
    }
}
