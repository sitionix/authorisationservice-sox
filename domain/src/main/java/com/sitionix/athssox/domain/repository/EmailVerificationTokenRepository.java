package com.sitionix.athssox.domain.repository;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;

import java.util.Optional;

public interface EmailVerificationTokenRepository {

    void save(final EmailVerificationTokenRecord tokenRecord);

    Optional<EmailVerificationTokenRecord> findByHashedToken(final String hashedToken);

}
