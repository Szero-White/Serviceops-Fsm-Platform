package com.serviceops.workorder.domain;

import com.serviceops.common.domain.TenantScopedEntity;
import com.serviceops.inventory.domain.SparePart;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "work_order_billing_items")
public class WorkOrderBillingItem extends TenantScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_snapshot_id", nullable = false)
    private WorkOrderBillingSnapshot billingSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spare_part_id", nullable = false)
    private SparePart sparePart;

    @Column(name = "spare_part_sku", nullable = false, length = 60)
    private String sparePartSku;

    @Column(name = "spare_part_name", nullable = false, length = 180)
    private String sparePartName;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;
}
