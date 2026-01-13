package com.sitionix.athssox.postgresql.jpa.token;

import com.sitionix.athssox.postgresql.entity.token.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {
}
