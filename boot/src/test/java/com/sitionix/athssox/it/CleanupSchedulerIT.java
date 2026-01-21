package com.sitionix.athssox.it;

import com.sitionix.athssox.application.cleanup.CleanupScheduler;
import com.sitionix.athssox.it.infra.DatabaseContract;
import com.sitionix.athssox.it.infra.TestManager;
import com.sitionix.forgeit.core.test.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(properties = {
        "auth.cleanup.refresh-tokens.retention-days=1",
        "auth.cleanup.email-verification-tokens.retention-days=1",
        "auth.cleanup.outbox-events.retention-days=1",
        "auth.cleanup.device-sessions.retention-days=0"
})
class CleanupSchedulerIT {

    @Autowired
    private TestManager testManager;

    @Autowired
    private CleanupScheduler cleanupScheduler;

    @Test
    @DisplayName("Should delete expired tokens and sent outbox events while keeping active records")
    void given_expired_records_when_cleanup_runs_then_delete_expired_and_keep_active() {
        //given
        this.testManager.postgresql()
                .create()
                .to(DatabaseContract.USER_STATUS_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.GLOBAL_ROLE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.USER_ENTITY_DB_CONTRACT.withJson("authUserActive.json"))
                .to(DatabaseContract.SESSION_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.DEVICE_SESSION_ENTITY_DB_CONTRACT.withJson("deviceSessionActive.json"))
                .to(DatabaseContract.REFRESH_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenActiveBoundToDevice123.json"))
                .to(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT.withJson("refreshTokenExpired.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenValid.json"))
                .to(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT.withJson("emailVerificationTokenExpired.json"))
                .to(DatabaseContract.OUTBOX_AGGREGATE_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_EVENT_TYPE_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_STATUS_ENTITY_DB_CONTRACT.getById(1L))
                .to(DatabaseContract.OUTBOX_STATUS_ENTITY_DB_CONTRACT.getById(3L))
                .to(DatabaseContract.OUTBOX_INITIATOR_TYPE_ENTITY_DB_CONTRACT.getById(2L))
                .to(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT.withJson("outboxEventEmailVerifyEntityPending.json"))
                .to(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT.withJson("outboxEventEmailVerifyEntitySentOld.json"))
                .build();

        //when
        this.cleanupScheduler.runCleanup();

        //then
        this.testManager.postgresql()
                .assertEntities(DatabaseContract.REFRESH_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1);

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.EMAIL_VERIFICATION_TOKEN_ENTITY_DB_CONTRACT)
                .hasSize(1);

        this.testManager.postgresql()
                .assertEntities(DatabaseContract.OUTBOX_EVENT_ENTITY_DB_CONTRACT)
                .hasSize(1);
    }
}
