package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.inventory.domain.InventoryTransactionRepository;
import com.serviceops.inventory.domain.WorkOrderPartRequestRepository;
import com.serviceops.inventory.domain.WorkOrderPartUsageRepository;
import com.serviceops.payment.domain.PaymentReceiptRepository;
import com.serviceops.payment.domain.PaymentRepository;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderActivityService {
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final WorkOrderPartRequestRepository partRequestRepository;
    private final WorkOrderPartUsageRepository partUsageRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentReceiptRepository receiptRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WorkOrderActivityResponse> timeline(UUID workOrderId) {
        requireViewableWorkOrder(workOrderId);
        UUID tenantId = CurrentUser.tenantId();
        var statusHistory = historyRepository.findByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(tenantId, workOrderId);
        var transactions = inventoryTransactionRepository.findTimelinePartTransactionsForWorkOrder(tenantId, workOrderId);
        var dispatchEvents = auditService.findEntityEvents(workOrderId, "WORK_ORDER", List.of("RESCHEDULE"));
        var requests = partRequestRepository.findDetailedByWorkOrder(tenantId, workOrderId);
        var usages = partUsageRepository.findDetailedByWorkOrder(tenantId, workOrderId);
        var payment = paymentRepository.findDetailedByWorkOrder(tenantId, workOrderId).orElse(null);
        var receipt = receiptRepository.findByWorkOrder(tenantId, workOrderId).orElse(null);
        return WorkOrderActivityMapper.mergeComplete(
                statusHistory,
                transactions,
                dispatchEvents,
                requests,
                usages,
                payment,
                receipt
        );
    }

    private void requireViewableWorkOrder(UUID workOrderId) {
        var workOrder = CurrentUser.hasRole("TECHNICIAN")
                ? workOrderRepository.findDetailedAssigned(workOrderId, CurrentUser.tenantId(), CurrentUser.userId())
                : workOrderRepository.findDetailed(workOrderId, CurrentUser.tenantId());
        if (workOrder.isEmpty()) {
            throw BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc");
        }
        if (!CurrentUser.hasRole("OWNER")
                && !CurrentUser.hasRole("DISPATCHER")
                && !CurrentUser.hasRole("CUSTOMER_SERVICE")
                && !CurrentUser.hasRole("TECHNICIAN")) {
            throw BusinessException.forbidden("WORK_ORDER_ACTIVITY_DENIED", "Bạn không có quyền xem tiến trình phiếu công việc");
        }
    }
}
