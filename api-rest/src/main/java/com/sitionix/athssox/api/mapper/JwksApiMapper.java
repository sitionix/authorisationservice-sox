package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.JwksResponseDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.jwks.JwksResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {
                JwkApiMapper.class
        })
public interface JwksApiMapper {

    JwksResponseDTO asJwksResponseDTO(JwksResponse response);
}
