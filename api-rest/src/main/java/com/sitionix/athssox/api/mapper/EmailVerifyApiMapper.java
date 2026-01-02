package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.EmailVerificationDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.emailverify.EmailVerification;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface EmailVerifyApiMapper {

    EmailVerification asEmailVerification(EmailVerificationDTO token);
}
