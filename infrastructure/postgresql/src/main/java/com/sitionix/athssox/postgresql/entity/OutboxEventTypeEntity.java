package com.sitionix.athssox.postgresql.entity;

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
@Table(name = "outbox_event_types")
public class OutboxEventTypeEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "description")
    private String description;
}
