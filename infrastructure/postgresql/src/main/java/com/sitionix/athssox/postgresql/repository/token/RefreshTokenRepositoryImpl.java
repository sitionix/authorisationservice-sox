package com.sitionix.athssox.postgresql.repository.token;

import com.sitionix.athssox.domain.model.RefreshTokenRecord;
import com.sitionix.athssox.domain.model.RefreshTokenStatus;
import com.sitionix.athssox.domain.repository.RefreshTokenRepository;
import com.sitionix.athssox.postgresql.entity.token.RefreshTokenEntity;
import com.sitionix.athssox.postgresql.jpa.token.RefreshTokenJpaRepository;
import com.sitionix.athssox.postgresql.mapper.token.RefreshTokenInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final RefreshTokenInfraMapper refreshTokenInfraMapper;

    @Override
    public void save(final RefreshTokenRecord refreshTokenRecord) {
        final RefreshTokenEntity entity = this.refreshTokenInfraMapper.asRefreshTokenEntity(refreshTokenRecord);

        this.refreshTokenJpaRepository.save(entity);
    }

    @Override
    public Optional<RefreshTokenRecord> findByTokenHash(final String tokenHash) {
        return this.refreshTokenJpaRepository.findByTokenHash(tokenHash)
                .map(this.refreshTokenInfraMapper::asRefreshTokenRecord);
    }

    @Override
    public boolean revokeIfActive(final Long tokenId, final Instant now, final String reason) {
        final int updated = this.refreshTokenJpaRepository.revokeIfActive(tokenId,
                RefreshTokenStatus.REVOKED.getId(),
                RefreshTokenStatus.ACTIVE.getId(),
                now,
                reason);
        return updated > 0;
    }

    @Override
    public void revokeActiveBySessionId(final UUID sessionId, final Instant now, final String reason) {
        this.refreshTokenJpaRepository.revokeActiveBySessionId(sessionId,
                RefreshTokenStatus.REVOKED.getId(),
                RefreshTokenStatus.ACTIVE.getId(),
                now,
                reason);
    }
}
