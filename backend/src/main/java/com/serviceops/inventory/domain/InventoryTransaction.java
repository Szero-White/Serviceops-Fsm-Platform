package com.serviceops.inventory.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import com.serviceops.workorder.domain.WorkOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction extends TenantScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spare_part_id", nullable = false)
    private SparePart sparePart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private InventoryTransactionType transactionType;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "balance_after", nullable = false, precision = 18, scale = 3)
    private BigDecimal balanceAfter;

    @Column(length = 300)
    private String note;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
}
