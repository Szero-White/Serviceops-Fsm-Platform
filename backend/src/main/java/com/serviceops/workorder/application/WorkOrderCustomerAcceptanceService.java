package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.payment.application.PaymentService;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderBillingSnapshot;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
import com.serviceops.workorder.web.WorkOrderBillingDtos.CustomerAcceptanceRequest;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderCustomerAcceptanceService {
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final WorkOrderBillingService billingService;
    private final PaymentService paymentService;
    private final AuditService auditService;
    private final WorkOrderService workOrderService;

    @Transactional
    public WorkOrderResponse accept(UUID workOrderId, CustomerAcceptanceRequest request) {
        if (!CurrentUser.hasRole("TECHNICIAN")) {
            throw BusinessException.forbidden(
                    "CUSTOMER_ACCEPTANCE_DENIED",
                    "Chỉ kỹ thuật viên được phân công mới ghi nhận xác nhận của khách tại hiện trường"
            );
        }
        WorkOrder workOrder = workOrderRepository.findForUpdate(workOrderId, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
        ensureAssignedTechnician(workOrder);
        if (workOrder.getStatus() != WorkOrderStatus.COMPLETED) {
            throw BusinessException.conflict(
                    "WORK_ORDER_NOT_READY_FOR_ACCEPTANCE",
                    "Chỉ phiếu đã hoàn thành mới được ghi nhận khách xác nhận"
            );
        }

        WorkOrderBillingSnapshot snapshot = billingService.freezeForCustomerAcceptance(workOrder);
        paymentService.initializeUnpaid(workOrder, snapshot);
        WorkOrderStatus previous = workOrder.getStatus();
        workOrder.transitionTo(WorkOrderStatus.CUSTOMER_ACCEPTED);

        WorkOrderStatusHistory history = new WorkOrderStatusHistory();
        history.setTenantId(workOrder.getTenantId());
        history.setWorkOrder(workOrder);
        history.setFromStatus(previous);
        history.setToStatus(WorkOrderStatus.CUSTOMER_ACCEPTED);
        history.setNote(blankToNull(request == null ? null : request.note()));
        history.setChangedBy(CurrentUser.username());
        history.setActorDisplayName(CurrentUser.displayName());
        history.setActorRole(CurrentUser.primaryRole());
        historyRepository.save(history);
        auditService.record(
                "CUSTOMER_ACCEPTANCE",
                "WORK_ORDER",
                workOrder.getId(),
                "Ghi nhận khách xác nhận và khóa chi phí " + workOrder.getCode()
        );
        return workOrderService.get(workOrderId);
    }

    private static void ensureAssignedTechnician(WorkOrder workOrder) {
        if (workOrder.getTechnician() == null
                || workOrder.getTechnician().getUser() == null
                || !CurrentUser.userId().equals(workOrder.getTechnician().getUser().getId())) {
            throw BusinessException.forbidden("WORK_ORDER_NOT_ASSIGNED", "Bạn chỉ được thao tác công việc được phân công cho mình");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
