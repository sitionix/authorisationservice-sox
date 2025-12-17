package com.sitionix.athssox.it.infra;

import com.sitionix.athssox.postgresql.entity.GlobalRoleEntity;
import com.sitionix.athssox.postgresql.entity.UserEntity;
import com.sitionix.athssox.postgresql.entity.UserStatusEntity;
import com.sitionix.forgeit.core.contract.ForgeDbContracts;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractsDsl;
import com.sitionix.forgeit.domain.contract.clean.CleanupPolicy;

@ForgeDbContracts
public class DatabaseContract {

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
}
