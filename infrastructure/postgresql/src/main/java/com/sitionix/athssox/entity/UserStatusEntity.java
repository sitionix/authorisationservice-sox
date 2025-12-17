package com.sitionix.athssox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "user_statuses")
public class UserStatusEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "DESCRIPTION")
    private String description;
}
