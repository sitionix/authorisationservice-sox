package com.sitionix.athssox.postgresql.repository.token;

import com.sitionix.athssox.domain.model.RefreshTokenRecord;
import com.sitionix.athssox.domain.model.RefreshTokenStatus;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import com.sitionix.athssox.postgresql.entity.token.RefreshTokenEntity;
import com.sitionix.athssox.postgresql.jpa.token.RefreshTokenJpaRepository;
import com.sitionix.athssox.postgresql.mapper.token.RefreshTokenInfraMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryImplTest {

    private RefreshTokenRepository repository;

    @Mock
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Mock
    private RefreshTokenInfraMapper refreshTokenInfraMapper;

    @BeforeEach
    void setUp() {
        this.repository = new RefreshTokenRepositoryImpl(
                this.refreshTokenJpaRepository,
                this.refreshTokenInfraMapper
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
                this.refreshTokenJpaRepository,
                this.refreshTokenInfraMapper
        );
    }

    @Test
    void given_refresh_token_record_when_save_then_verify() {
        //given
        final RefreshTokenRecord refreshTokenRecord = mock(RefreshTokenRecord.class);
        final RefreshTokenEntity entity = mock(RefreshTokenEntity.class);

        when(this.refreshTokenInfraMapper.asRefreshTokenEntity(refreshTokenRecord))
                .thenReturn(entity);

        //when
        this.repository.save(refreshTokenRecord);

        //then
        verify(this.refreshTokenInfraMapper)
                .asRefreshTokenEntity(refreshTokenRecord);
        verify(this.refreshTokenJpaRepository)
                .save(entity);
    }

    @Test
    void given_token_hash_when_find_by_token_hash_then_return_refresh_token_record() {
        //given
        final String tokenHash = this.getTokenHash();
        final RefreshTokenEntity entity = mock(RefreshTokenEntity.class);
        final RefreshTokenRecord refreshTokenRecord = mock(RefreshTokenRecord.class);

        when(this.refreshTokenJpaRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(entity));
        when(this.refreshTokenInfraMapper.asRefreshTokenRecord(entity))
                .thenReturn(refreshTokenRecord);

        //when
        final Optional<RefreshTokenRecord> actual = this.repository.findByTokenHash(tokenHash);

        //then
        assertThat(actual).isEqualTo(Optional.of(refreshTokenRecord));
        verify(this.refreshTokenJpaRepository)
                .findByTokenHash(tokenHash);
        verify(this.refreshTokenInfraMapper)
                .asRefreshTokenRecord(entity);
    }

    @Test
    void given_active_token_when_revoke_if_active_then_return_true() {
        //given
        final Long tokenId = this.getTokenId();
        final Instant now = this.getNow();
        final String reason = this.getReason();
        final Long revokedStatusId = RefreshTokenStatus.REVOKED.getId();
        final Long activeStatusId = RefreshTokenStatus.ACTIVE.getId();

        when(this.refreshTokenJpaRepository.revokeIfActive(tokenId, revokedStatusId, activeStatusId, now, reason))
                .thenReturn(1);

        //when
        final boolean actual = this.repository.revokeIfActive(tokenId, now, reason);

        //then
        assertThat(actual).isEqualTo(true);
        verify(this.refreshTokenJpaRepository)
                .revokeIfActive(tokenId, revokedStatusId, activeStatusId, now, reason);
    }

    @Test
    void given_inactive_token_when_revoke_if_active_then_return_false() {
        //given
        final Long tokenId = this.getTokenId();
        final Instant now = this.getNow();
        final String reason = this.getReason();
        final Long revokedStatusId = RefreshTokenStatus.REVOKED.getId();
        final Long activeStatusId = RefreshTokenStatus.ACTIVE.getId();

        when(this.refreshTokenJpaRepository.revokeIfActive(tokenId, revokedStatusId, activeStatusId, now, reason))
                .thenReturn(0);

        //when
        final boolean actual = this.repository.revokeIfActive(tokenId, now, reason);

        //then
        assertThat(actual).isEqualTo(false);
        verify(this.refreshTokenJpaRepository)
                .revokeIfActive(tokenId, revokedStatusId, activeStatusId, now, reason);
    }

    @Test
    void given_session_id_when_revoke_active_by_session_id_then_verify() {
        //given
        final UUID sessionId = this.getSessionId();
        final Instant now = this.getNow();
        final String reason = this.getReason();
        final Long revokedStatusId = RefreshTokenStatus.REVOKED.getId();
        final Long activeStatusId = RefreshTokenStatus.ACTIVE.getId();

        //when
        this.repository.revokeActiveBySessionId(sessionId, now, reason);

        //then
        verify(this.refreshTokenJpaRepository)
                .revokeActiveBySessionId(sessionId, revokedStatusId, activeStatusId, now, reason);
    }

    private String getTokenHash() {
        return "token-hash";
    }

    private Long getTokenId() {
        return 10L;
    }

    private UUID getSessionId() {
        return UUID.fromString("2b2f2a92-7c85-4b02-8010-6f1b4d79f2ed");
    }

    private Instant getNow() {
        return Instant.parse("2024-02-10T09:00:00Z");
    }

    private String getReason() {
        return "revoked";
    }
}
