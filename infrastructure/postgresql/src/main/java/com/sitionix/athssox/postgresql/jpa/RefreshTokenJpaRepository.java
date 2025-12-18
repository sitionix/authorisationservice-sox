package com.sitionix.athssox.postgresql.jpa;

import com.sitionix.athssox.postgresql.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {
}
