package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.JwkDTO;
import com.app_afesox.athssox.api_first.dto.JwksResponseDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.jwks.JwkKey;
import com.sitionix.athssox.domain.model.jwks.JwksResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface JwksApiMapper {

    JwksResponseDTO asJwksResponseDTO(JwksResponse response);

    JwkDTO asJwkDTO(JwkKey key);
}
