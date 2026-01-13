package com.sitionix.athssox.postgresql.entity.session;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SESSION_STATUSES")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionStatusEntity {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "DESCRIPTION")
    private String description;
}
