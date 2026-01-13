package com.sitionix.athssox.postgresql.mapper.token;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.RefreshTokenRecord;
import com.sitionix.athssox.postgresql.entity.token.RefreshTokenEntity;
import com.sitionix.athssox.postgresql.mapper.session.DeviceSessionInfraMapper;
import com.sitionix.athssox.postgresql.mapper.user.UserInfraMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {UserInfraMapper.class, DeviceSessionInfraMapper.class, RefreshTokenStatusInfraMapper.class})
public interface RefreshTokenInfraMapper {

    RefreshTokenEntity asRefreshTokenEntity(final RefreshTokenRecord refreshTokenRecord);

    RefreshTokenRecord asRefreshTokenRecord(final RefreshTokenEntity refreshTokenEntity);
}
