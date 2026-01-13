package com.sitionix.athssox.postgresql.entity.user;

import com.sitionix.athssox.postgresql.entity.session.DeviceSessionEntity;
import com.sitionix.athssox.postgresql.entity.token.RefreshTokenEntity;
import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email")
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, referencedColumnName = "id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private GlobalRoleEntity globalRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false, referencedColumnName = "id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private UserStatusEntity status;

    @Column(name = "password_hash")
    private String passwordHash;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<DeviceSessionEntity> deviceSessions = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<RefreshTokenEntity> refreshTokens = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "site_id")
    private UUID siteId;
}
