package com.sitionix.athssox.postgresql.jpa.token;

import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationTokenEntity> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);

    Optional<EmailVerificationTokenEntity> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);

    long countByUser_IdAndCreatedAtAfter(Long userId, Instant createdAfter);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE email_verification_tokens " +
            "SET status_id = :revokedStatusId " +
            "WHERE user_id = :userId AND status_id = :activeStatusId",
            nativeQuery = true)
    int revokeActiveByUserId(@Param("userId") Long userId,
                             @Param("activeStatusId") Long activeStatusId,
                             @Param("revokedStatusId") Long revokedStatusId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM email_verification_tokens " +
            "WHERE expires_at < :cutoff",
            nativeQuery = true)
    int deleteExpiredBefore(final Instant cutoff);
}
