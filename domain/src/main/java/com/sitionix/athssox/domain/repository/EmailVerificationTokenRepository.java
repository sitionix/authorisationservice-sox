package com.sitionix.athssox.domain.repository;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository {

    void save(final EmailVerificationTokenRecord tokenRecord);

    Optional<EmailVerificationTokenRecord> findByHashedToken(final String hashedToken);

    Optional<Instant> findLatestCreatedAtByUserId(final Long userId);

    long countByUserIdAndCreatedAtAfter(final Long userId, final Instant createdAfter);

    int deleteExpiredBefore(final Instant cutoff);
}
