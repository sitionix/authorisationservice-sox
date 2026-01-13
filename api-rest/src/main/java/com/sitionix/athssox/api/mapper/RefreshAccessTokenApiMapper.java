package com.sitionix.athssox.api.mapper;

import com.app_afesox.athssox.api_first.dto.RefreshAccessTokenRequestDTO;
import com.app_afesox.athssox.api_first.dto.RefreshAccessTokenResponseDTO;
import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.RefreshAccessTokenRequest;
import com.sitionix.athssox.domain.model.RefreshAccessTokenResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT)
public interface RefreshAccessTokenApiMapper {

    RefreshAccessTokenRequest asRefreshAccessTokenRequest(RefreshAccessTokenRequestDTO dto);

    RefreshAccessTokenResponseDTO asRefreshAccessTokenResponseDTO(RefreshAccessTokenResponse model);

}
