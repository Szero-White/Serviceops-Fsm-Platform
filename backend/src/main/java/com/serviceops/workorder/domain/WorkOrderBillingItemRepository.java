package com.serviceops.workorder.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkOrderBillingItemRepository extends JpaRepository<WorkOrderBillingItem, UUID> {
    List<WorkOrderBillingItem> findByTenantIdAndBillingSnapshotIdOrderBySparePartNameAsc(UUID tenantId, UUID billingSnapshotId);
}
