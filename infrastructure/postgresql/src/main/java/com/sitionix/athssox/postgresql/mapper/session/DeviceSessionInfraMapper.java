package com.sitionix.athssox.postgresql.mapper.session;

import com.sitionix.athssox.domain.config.MapstructComponent;
import com.sitionix.athssox.domain.model.DeviceSession;
import com.sitionix.athssox.postgresql.entity.session.DeviceSessionEntity;
import com.sitionix.athssox.postgresql.mapper.user.UserInfraMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = MapstructComponent.SPRING_COMPONENT,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {UserInfraMapper.class, SessionStatusInfraMapper.class})
public interface DeviceSessionInfraMapper {

    @Mapping(target = "refreshTokens", ignore = true)
    DeviceSessionEntity asDeviceSessionEntity(final DeviceSession deviceSession);

    DeviceSession asDeviceSession(final DeviceSessionEntity deviceSessionEntity);
}
