package com.serviceops.inventory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    Page<InventoryTransaction> findByTenantIdAndSparePartId(UUID tenantId, UUID sparePartId, Pageable pageable);
}
