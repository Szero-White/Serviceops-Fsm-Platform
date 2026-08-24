package com.serviceops.audit.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByTenantIdAndEntityTypeAndEntityIdAndActionInOrderByCreatedAtAsc(
            UUID tenantId,
            String entityType,
            UUID entityId,
            Collection<String> actions
    );
}
