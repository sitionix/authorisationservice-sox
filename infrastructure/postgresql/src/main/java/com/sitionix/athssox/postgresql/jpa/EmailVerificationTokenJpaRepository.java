package com.sitionix.athssox.postgresql.jpa;

import com.sitionix.athssox.postgresql.entity.EmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);
}
