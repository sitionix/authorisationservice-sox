package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.JwkDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.jwks.JwkKey;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface JwkApiMapper {

    JwkDTO asJwkDTO(JwkKey key);
}
