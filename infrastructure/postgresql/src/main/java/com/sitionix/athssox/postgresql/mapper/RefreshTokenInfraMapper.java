package com.sitionix.athssox.postgresql.mapper;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.RefreshTokenRecord;
import com.sitionix.athssox.postgresql.entity.RefreshTokenEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {UserInfraMapper.class})
public interface RefreshTokenInfraMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RefreshTokenEntity asRefreshTokenEntity(final RefreshTokenRecord refreshTokenRecord);
}
