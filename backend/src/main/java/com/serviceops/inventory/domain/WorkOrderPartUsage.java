package com.serviceops.inventory.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import com.serviceops.workorder.domain.WorkOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "work_order_part_usage")
public class WorkOrderPartUsage extends TenantScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spare_part_id", nullable = false)
    private SparePart sparePart;

    @Column(name = "used_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal usedQuantity;

    @Column(name = "updated_by_user_id", nullable = false)
    private UUID updatedByUserId;

    @Column(name = "updated_by_username", nullable = false, length = 100)
    private String updatedByUsername;

    @Column(name = "updated_by_display_name", nullable = false, length = 150)
    private String updatedByDisplayName;
}
