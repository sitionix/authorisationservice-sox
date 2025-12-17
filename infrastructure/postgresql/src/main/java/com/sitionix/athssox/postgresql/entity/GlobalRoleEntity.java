package com.sitionix.athssox.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "global_roles")
public class GlobalRoleEntity {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "DESCRIPTION")
    private String description;
}
