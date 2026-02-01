package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.Jwk;
import com.app_afesox.athssox.api_first.dto.JwksResponse;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.jwks.JwkKey;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface JwksApiMapper {

    JwksResponse asJwksResponseDTO(com.sitionix.athssox.domain.model.jwks.JwksResponse response);

    Jwk asJwk(JwkKey key);
}
