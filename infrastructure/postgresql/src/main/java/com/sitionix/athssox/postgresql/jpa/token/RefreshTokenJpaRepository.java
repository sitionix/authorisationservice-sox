package com.sitionix.athssox.postgresql.jpa.token;

import com.sitionix.athssox.postgresql.entity.token.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(final String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE refresh_tokens " +
            "SET status_id = :revokedStatusId, " +
            "    used_at = :now, " +
            "    revoked_at = :now, " +
            "    revoked_reason = :reason, " +
            "    updated_at = :now " +
            "WHERE id = :tokenId " +
            "  AND status_id = :activeStatusId " +
            "  AND used_at IS NULL " +
            "  AND revoked_at IS NULL",
            nativeQuery = true)
    int revokeIfActive(final Long tokenId,
                       final Long revokedStatusId,
                       final Long activeStatusId,
                       final Instant now,
                       final String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE refresh_tokens " +
            "SET status_id = :revokedStatusId, " +
            "    revoked_at = :now, " +
            "    revoked_reason = :reason, " +
            "    updated_at = :now " +
            "WHERE session_id = :sessionId " +
            "  AND status_id = :activeStatusId " +
            "  AND used_at IS NULL " +
            "  AND revoked_at IS NULL",
            nativeQuery = true)
    int revokeActiveBySessionId(final java.util.UUID sessionId,
                                final Long revokedStatusId,
                                final Long activeStatusId,
                                final Instant now,
                                final String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM refresh_tokens " +
            "WHERE (status_id = :revokedStatusId AND revoked_at < :cutoff) " +
            "   OR (expires_at < :cutoff)",
            nativeQuery = true)
    int deleteInactiveBefore(final Instant cutoff, final Long revokedStatusId);
}
