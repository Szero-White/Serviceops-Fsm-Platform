package com.serviceops.workorder.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkOrderTest {

    @Test
    void shouldFollowValidLifecycle() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setStatus(WorkOrderStatus.OPEN);

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
        WorkOrder workOrder = new WorkOrder();
        workOrder.setStatus(WorkOrderStatus.OPEN);

        assertThatThrownBy(() -> workOrder.transitionTo(WorkOrderStatus.COMPLETED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không thể chuyển trạng thái");
    }

    @Test
    void reopeningShouldClearCompletionTimestamp() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        workOrder.transitionTo(WorkOrderStatus.COMPLETED);
        workOrder.transitionTo(WorkOrderStatus.REOPENED);

        assertThat(workOrder.getCompletedAt()).isNull();
    }
}
