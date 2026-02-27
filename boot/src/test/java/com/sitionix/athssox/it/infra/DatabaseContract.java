package com.sitionix.athssox.it.infra;

import com.sitionix.athssox.postgresql.entity.user.GlobalRoleEntity;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenEntity;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenStatusEntity;
import com.sitionix.forge.outbox.postgres.entity.ForgeOutboxEventEntity;
import com.sitionix.athssox.postgresql.entity.session.DeviceSessionEntity;
import com.sitionix.athssox.postgresql.entity.session.SessionStatusEntity;
import com.sitionix.athssox.postgresql.entity.token.RefreshTokenEntity;
import com.sitionix.athssox.postgresql.entity.token.RefreshTokenStatusEntity;
import com.sitionix.athssox.postgresql.entity.user.UserEntity;
import com.sitionix.athssox.postgresql.entity.user.UserStatusEntity;
import com.sitionix.forgeit.core.contract.ForgeDbContracts;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractsDsl;
import com.sitionix.forgeit.domain.contract.clean.CleanupPolicy;

@ForgeDbContracts
public class DatabaseContract {

    public static final DbContract<EmailVerificationTokenStatusEntity> EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(EmailVerificationTokenStatusEntity.class)
                    .cleanupPolicy(CleanupPolicy.NONE)
                    .build();

    public static final DbContract<UserStatusEntity> USER_STATUS_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(UserStatusEntity.class)
                    .cleanupPolicy(CleanupPolicy.NONE)
                    .build();

    public static final DbContract<SessionStatusEntity> SESSION_STATUS_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(SessionStatusEntity.class)
                    .cleanupPolicy(CleanupPolicy.NONE)
                    .build();

    public static final DbContract<RefreshTokenStatusEntity> REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(RefreshTokenStatusEntity.class)
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

    public static final DbContract<DeviceSessionEntity> DEVICE_SESSION_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(DeviceSessionEntity.class)
                    .dependsOn(USER_ENTITY_DB_CONTRACT, DeviceSessionEntity::setUser)
                    .dependsOn(SESSION_STATUS_ENTITY_DB_CONTRACT, DeviceSessionEntity::setStatus)
                    .cleanupPolicy(CleanupPolicy.DELETE_ALL)
                    .build();

    public static final DbContract<ForgeOutboxEventEntity> FORGE_OUTBOX_EVENT_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(ForgeOutboxEventEntity.class)
                    .cleanupPolicy(CleanupPolicy.DELETE_ALL)
                    .build();

    public static final DbContract<RefreshTokenEntity> REFRESH_TOKEN_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(RefreshTokenEntity.class)
                    .dependsOn(USER_ENTITY_DB_CONTRACT, RefreshTokenEntity::setUser)
                    .dependsOn(DEVICE_SESSION_ENTITY_DB_CONTRACT, RefreshTokenEntity::setSession)
                    .dependsOn(REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT, RefreshTokenEntity::setStatus)
                    .cleanupPolicy(CleanupPolicy.DELETE_ALL)
                    .build();

    public static final DbContract<EmailVerificationTokenEntity> EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(EmailVerificationTokenEntity.class)
                    .dependsOn(USER_ENTITY_DB_CONTRACT, EmailVerificationTokenEntity::setUser)
                    .dependsOn(EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT, EmailVerificationTokenEntity::setStatus)
                    .cleanupPolicy(CleanupPolicy.DELETE_ALL)
                    .build();
}
