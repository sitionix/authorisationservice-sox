package com.sitionix.athssox.it.infra;

import com.sitionix.athssox.postgresql.entity.GlobalRoleEntity;
import com.sitionix.athssox.postgresql.entity.OutboxAggregateTypeEntity;
import com.sitionix.athssox.postgresql.entity.OutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.OutboxEventTypeEntity;
import com.sitionix.athssox.postgresql.entity.OutboxInitiatorTypeEntity;
import com.sitionix.athssox.postgresql.entity.OutboxStatusEntity;
import com.sitionix.athssox.postgresql.entity.RefreshTokenEntity;
import com.sitionix.athssox.postgresql.entity.UserEntity;
import com.sitionix.athssox.postgresql.entity.UserStatusEntity;
import com.sitionix.forgeit.core.contract.ForgeDbContracts;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractsDsl;
import com.sitionix.forgeit.domain.contract.clean.CleanupPolicy;

@ForgeDbContracts
public class DatabaseContract {

    public static final DbContract<OutboxAggregateTypeEntity> OUTBOX_AGGREGATE_TYPE_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(OutboxAggregateTypeEntity.class)
                    .cleanupPolicy(CleanupPolicy.NONE)
                    .build();

    public static final DbContract<OutboxEventTypeEntity> OUTBOX_EVENT_TYPE_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(OutboxEventTypeEntity.class)
                    .cleanupPolicy(CleanupPolicy.NONE)
                    .build();

    public static final DbContract<OutboxStatusEntity> OUTBOX_STATUS_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(OutboxStatusEntity.class)
                    .cleanupPolicy(CleanupPolicy.NONE)
                    .build();

    public static final DbContract<OutboxInitiatorTypeEntity> OUTBOX_INITIATOR_TYPE_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(OutboxInitiatorTypeEntity.class)
                    .cleanupPolicy(CleanupPolicy.NONE)
                    .build();

    public static final DbContract<UserStatusEntity> USER_STATUS_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(UserStatusEntity.class)
                    .cleanupPolicy(CleanupPolicy.NONE)
                    .build();

    public static final DbContract<GlobalRoleEntity> GLOBAL_ROLE_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(GlobalRoleEntity.class)
                    .cleanupPolicy(CleanupPolicy.NONE)
                    .build();

    public static final DbContract<UserEntity> USER_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(UserEntity.class)
                    .dependsOn(USER_STATUS_ENTITY_DB_CONTRACT, UserEntity::setStatus)
                    .dependsOn(GLOBAL_ROLE_ENTITY_DB_CONTRACT, UserEntity::setGlobalRole)
                    .withDefaultBody("defaultUserEntity.json")
                    .cleanupPolicy(CleanupPolicy.DELETE_ALL)
                    .build();

    public static final DbContract<OutboxEventEntity> OUTBOX_EVENT_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(OutboxEventEntity.class)
                    .dependsOn(OUTBOX_AGGREGATE_TYPE_ENTITY_DB_CONTRACT, OutboxEventEntity::setAggregateType)
                    .dependsOn(OUTBOX_EVENT_TYPE_ENTITY_DB_CONTRACT, OutboxEventEntity::setEventType)
                    .dependsOn(OUTBOX_STATUS_ENTITY_DB_CONTRACT, OutboxEventEntity::setStatus)
                    .dependsOn(OUTBOX_INITIATOR_TYPE_ENTITY_DB_CONTRACT, OutboxEventEntity::setInitiatorType)
                    .cleanupPolicy(CleanupPolicy.DELETE_ALL)
                    .build();

    public static final DbContract<RefreshTokenEntity> REFRESH_TOKEN_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(RefreshTokenEntity.class)
                    .cleanupPolicy(CleanupPolicy.DELETE_ALL)
                    .build();
}
