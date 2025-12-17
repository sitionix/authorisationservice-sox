package com.sitionix.athssox.postgresql.jpa;

import com.sitionix.athssox.postgresql.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmailAndSiteIdAndGlobalRole_IdIn(final String email,
                                                     final UUID siteId,
                                                     final Collection<Long> globalRoleIds);

    boolean existsByEmailAndGlobalRole_IdIn(final String email,
                                            final Collection<Long> globalRoleIds);
}
