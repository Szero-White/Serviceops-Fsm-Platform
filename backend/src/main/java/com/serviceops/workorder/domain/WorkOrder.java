package com.serviceops.workorder.domain;

import com.serviceops.asset.domain.Asset;
import com.serviceops.common.domain.Priority;
import com.serviceops.common.domain.TenantScopedEntity;
import com.serviceops.customer.domain.Customer;
import com.serviceops.servicerequest.domain.ServiceRequest;
import com.serviceops.technician.domain.TechnicianProfile;
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

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "work_orders")
public class WorkOrder extends TenantScopedEntity {
    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(WorkOrderStatus.DRAFT, EnumSet.of(WorkOrderStatus.OPEN, WorkOrderStatus.CANCELLED)),
            Map.entry(WorkOrderStatus.OPEN, EnumSet.of(WorkOrderStatus.SCHEDULED, WorkOrderStatus.CANCELLED)),
            Map.entry(WorkOrderStatus.SCHEDULED, EnumSet.of(WorkOrderStatus.ASSIGNED, WorkOrderStatus.OPEN, WorkOrderStatus.CANCELLED)),
            Map.entry(WorkOrderStatus.ASSIGNED, EnumSet.of(WorkOrderStatus.ON_THE_WAY, WorkOrderStatus.SCHEDULED, WorkOrderStatus.CANCELLED)),
            Map.entry(WorkOrderStatus.ON_THE_WAY, EnumSet.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED)),
            Map.entry(WorkOrderStatus.IN_PROGRESS, EnumSet.of(WorkOrderStatus.WAITING_FOR_PARTS, WorkOrderStatus.COMPLETED, WorkOrderStatus.CANCELLED)),
            Map.entry(WorkOrderStatus.WAITING_FOR_PARTS, EnumSet.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED)),
            Map.entry(WorkOrderStatus.COMPLETED, EnumSet.of(WorkOrderStatus.CUSTOMER_ACCEPTED, WorkOrderStatus.REOPENED)),
            Map.entry(WorkOrderStatus.CUSTOMER_ACCEPTED, EnumSet.of(WorkOrderStatus.CLOSED, WorkOrderStatus.REOPENED)),
            Map.entry(WorkOrderStatus.REOPENED, EnumSet.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED)),
            Map.entry(WorkOrderStatus.CLOSED, EnumSet.noneOf(WorkOrderStatus.class)),
            Map.entry(WorkOrderStatus.CANCELLED, EnumSet.noneOf(WorkOrderStatus.class))
    );

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private TechnicianProfile technician;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 200)
    private String summary;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WorkOrderStatus status = WorkOrderStatus.OPEN;

    @Column(name = "scheduled_start")
    private Instant scheduledStart;

    @Column(name = "scheduled_end")
    private Instant scheduledEnd;

    @Column(columnDefinition = "text")
    private String diagnosis;

    @Column(columnDefinition = "text")
    private String resolution;

    @Column(name = "completed_at")
    private Instant completedAt;

    public WorkOrderStatus transitionTo(WorkOrderStatus target) {
        Set<WorkOrderStatus> allowed = TRANSITIONS.getOrDefault(status, Set.of());
        if (!allowed.contains(target)) {
            throw new IllegalStateException("Không thể chuyển trạng thái từ " + status + " sang " + target);
        }
        WorkOrderStatus previous = status;
        status = target;
        if (target == WorkOrderStatus.COMPLETED) {
            completedAt = Instant.now();
        }
        if (target == WorkOrderStatus.REOPENED || target == WorkOrderStatus.IN_PROGRESS) {
            completedAt = null;
        }
        return previous;
    }

    public void schedule(TechnicianProfile technician, Instant start, Instant end) {
        if (end == null || start == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (status == WorkOrderStatus.OPEN) {
            transitionTo(WorkOrderStatus.SCHEDULED);
        } else if (status != WorkOrderStatus.SCHEDULED && status != WorkOrderStatus.ASSIGNED) {
            throw new IllegalStateException("Chỉ work order mở, đã lên lịch hoặc đã phân công mới được xếp lịch");
        }
        this.technician = technician;
        this.scheduledStart = start;
        this.scheduledEnd = end;
        this.status = WorkOrderStatus.ASSIGNED;
    }
}
