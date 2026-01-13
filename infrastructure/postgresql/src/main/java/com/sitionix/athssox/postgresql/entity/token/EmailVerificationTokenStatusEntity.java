package com.sitionix.athssox.postgresql.entity.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "email_verification_token_statuses")
public class EmailVerificationTokenStatusEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "description")
    private String description;
}
