package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.ResendEmailVerificationResponseDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.ResendEmailVerificationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface ResendEmailVerificationApiMapper {

    ResendEmailVerificationResponseDTO asResendEmailVerificationResponseDTO(final ResendEmailVerificationResponse src);
}
