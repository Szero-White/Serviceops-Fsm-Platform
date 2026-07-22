package com.serviceops.workorder.domain;

import com.serviceops.common.domain.TenantScopedEntity;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "work_order_status_history")
public class WorkOrderStatusHistory extends TenantScopedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 40)
    private WorkOrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 40)
    private WorkOrderStatus toStatus;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;
}
