package com.serviceops.audit.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog extends TenantScopedEntity {
    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(columnDefinition = "text")
    private String details;
}
