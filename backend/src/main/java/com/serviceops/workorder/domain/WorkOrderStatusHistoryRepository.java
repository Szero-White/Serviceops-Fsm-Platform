package com.serviceops.workorder.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkOrderStatusHistoryRepository extends JpaRepository<WorkOrderStatusHistory, UUID> {
    List<WorkOrderStatusHistory> findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(UUID tenantId, UUID workOrderId);
}
