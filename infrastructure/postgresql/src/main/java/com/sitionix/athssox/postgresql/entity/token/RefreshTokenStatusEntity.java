package com.sitionix.athssox.postgresql.entity.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_token_statuses")
public class RefreshTokenStatusEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;
    @Column(name = "description", nullable = false, length = 256)
    private String description;
}
