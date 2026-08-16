package com.serviceops.workorder.domain;

import com.serviceops.technician.domain.TechnicianProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkOrderTest {

    @Test
    void shouldFollowValidLifecycle() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.OPEN);

        workOrder.transitionTo(WorkOrderStatus.SCHEDULED);
        workOrder.transitionTo(WorkOrderStatus.ASSIGNED);
        workOrder.transitionTo(WorkOrderStatus.ON_THE_WAY);
        workOrder.transitionTo(WorkOrderStatus.IN_PROGRESS);
        workOrder.transitionTo(WorkOrderStatus.COMPLETED);

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
        assertThat(workOrder.getCompletedAt()).isNotNull();
    }

    @Test
    void shouldRejectInvalidTransition() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.OPEN);

        assertThatThrownBy(() -> workOrder.transitionTo(WorkOrderStatus.COMPLETED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không thể chuyển trạng thái");
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
    }

    @Test
    void reopeningShouldClearCompletionTimestamp() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.IN_PROGRESS);
        workOrder.transitionTo(WorkOrderStatus.COMPLETED);
        workOrder.transitionTo(WorkOrderStatus.REOPENED);

        assertThat(workOrder.getCompletedAt()).isNull();
    }

    @Test
    void schedulingOpenWorkOrderShouldAssignTechnicianAndTimeWindow() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.OPEN);
        TechnicianProfile technician = new TechnicianProfile();
        Instant start = Instant.parse("2026-08-16T01:00:00Z");
        Instant end = start.plusSeconds(3600);

        workOrder.schedule(technician, start, end);

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.ASSIGNED);
        assertThat(workOrder.getTechnician()).isSameAs(technician);
        assertThat(workOrder.getScheduledStart()).isEqualTo(start);
        assertThat(workOrder.getScheduledEnd()).isEqualTo(end);
    }

    @Test
    void schedulingShouldRejectInvalidTimeWindowWithoutChangingState() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.OPEN);
        Instant start = Instant.parse("2026-08-16T01:00:00Z");

        assertThatThrownBy(() -> workOrder.schedule(new TechnicianProfile(), start, start))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Thời gian kết thúc");
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
    }

    @Test
    void schedulingShouldRejectCompletedWorkOrder() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.COMPLETED);
        Instant start = Instant.parse("2026-08-16T01:00:00Z");

        assertThatThrownBy(() -> workOrder.schedule(new TechnicianProfile(), start, start.plusSeconds(3600)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Chỉ phiếu công việc");
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
    }

    @Test
    void closedWorkOrderShouldNotAllowFurtherTransition() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.CLOSED);

        assertThatThrownBy(() -> workOrder.transitionTo(WorkOrderStatus.REOPENED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.CLOSED);
    }

    @Test
    void cancelledWorkOrderCanBeReopened() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.CANCELLED);

        workOrder.transitionTo(WorkOrderStatus.REOPENED);

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.REOPENED);
    }

    @Test
    void softDeleteShouldOnlyAllowClosedOrCancelledWorkOrder() {
        WorkOrder open = workOrder(WorkOrderStatus.OPEN);
        WorkOrder closed = workOrder(WorkOrderStatus.CLOSED);

        assertThatThrownBy(() -> open.softDelete("owner"))
                .isInstanceOf(IllegalStateException.class);

        closed.softDelete("owner");
        assertThat(closed.getDeletedAt()).isNotNull();
        assertThat(closed.getDeletedBy()).isEqualTo("owner");
    }

    @Test
    void acceptedWorkOrderCanBeClosed() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.CUSTOMER_ACCEPTED);

        workOrder.transitionTo(WorkOrderStatus.CLOSED);

        assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.CLOSED);
    }

    private static WorkOrder workOrder(WorkOrderStatus status) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setStatus(status);
        return workOrder;
    }
}
