package com.sitionix.athssox.domain.repository;

import com.sitionix.athssox.domain.model.RefreshTokenRecord;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    void save(final RefreshTokenRecord refreshTokenRecord);

    Optional<RefreshTokenRecord> findByTokenHash(final String tokenHash);

    boolean revokeIfActive(final Long tokenId, final Instant now, final String reason);

    int revokeActiveBySessionId(final UUID sessionId, final Instant now, final String reason);

    int deleteInactiveBefore(final Instant cutoff);
}
