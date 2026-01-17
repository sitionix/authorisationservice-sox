package com.sitionix.athssox.domain.repository;

import com.sitionix.athssox.domain.model.RefreshTokenRecord;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository {

    void save(final RefreshTokenRecord refreshTokenRecord);

    Optional<RefreshTokenRecord> findByTokenHash(final String tokenHash);

    boolean revokeIfActive(final Long tokenId, final Instant now, final String reason);
}
