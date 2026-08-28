package com.serviceops.workorder.application;

import com.serviceops.audit.application.AuditService;
import com.serviceops.common.exception.BusinessException;
import com.serviceops.identity.domain.UserRole;
import com.serviceops.notification.application.NotificationCopy;
import com.serviceops.notification.application.NotificationService;
import com.serviceops.payment.application.PaymentReceiptService;
import com.serviceops.payment.domain.Payment;
import com.serviceops.payment.domain.PaymentRepository;
import com.serviceops.payment.domain.PaymentStatus;
import com.serviceops.security.CurrentUser;
import com.serviceops.workorder.domain.WorkOrder;
import com.serviceops.workorder.domain.WorkOrderRepository;
import com.serviceops.workorder.domain.WorkOrderStatus;
import com.serviceops.workorder.domain.WorkOrderStatusHistory;
import com.serviceops.workorder.domain.WorkOrderStatusHistoryRepository;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderClosureService {
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentReceiptService paymentReceiptService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final WorkOrderService workOrderService;

    @Transactional
    public WorkOrderResponse close(UUID workOrderId) {
        requireCustomerServiceRole();
        WorkOrder workOrder = workOrderRepository.findForUpdate(workOrderId, CurrentUser.tenantId())
                .orElseThrow(() -> BusinessException.notFound("WORK_ORDER_NOT_FOUND", "Không tìm thấy phiếu công việc"));
        if (workOrder.getStatus() != WorkOrderStatus.CUSTOMER_ACCEPTED) {
            throw BusinessException.conflict(
                    "WORK_ORDER_NOT_READY_TO_CLOSE",
                    "Chỉ phiếu đã được khách xác nhận mới có thể đóng"
            );
        }

        Payment payment = paymentRepository.findForUpdateByWorkOrder(CurrentUser.tenantId(), workOrderId)
                .orElseThrow(() -> BusinessException.conflict(
                        "PAYMENT_REQUIRED_FOR_CLOSURE",
                        "Phiếu phải có khoản thanh toán trước khi đóng"
                ));
        if (payment.getStatus() != PaymentStatus.SETTLED) {
            throw BusinessException.conflict(
                    "PAYMENT_NOT_SETTLED",
                    "Chỉ được đóng phiếu sau khi CSKH xác nhận tiền đã về công ty"
            );
        }

        // Ensure every normally closed Work Order has an official receipt available in history.
        // issue() is idempotent, so an already-issued receipt is reused.
        paymentReceiptService.issue(workOrderId);

        workOrder.transitionTo(WorkOrderStatus.CLOSED);
        historyRepository.save(history(workOrder));
        auditService.record(
                "CLOSE_WORK_ORDER",
                "WORK_ORDER",
                workOrder.getId(),
                "Đóng " + workOrder.getCode() + " sau khi thanh toán đã đối soát"
        );
        notifyClosure(workOrder);
        return workOrderService.get(workOrderId);
    }

    private static WorkOrderStatusHistory history(WorkOrder workOrder) {
        WorkOrderStatusHistory history = new WorkOrderStatusHistory();
        history.setTenantId(workOrder.getTenantId());
        history.setWorkOrder(workOrder);
        history.setFromStatus(WorkOrderStatus.CUSTOMER_ACCEPTED);
        history.setToStatus(WorkOrderStatus.CLOSED);
        history.setNote("Thanh toán đã đối soát; hoàn tất quy trình dịch vụ");
        history.setChangedBy(CurrentUser.username());
        history.setActorDisplayName(CurrentUser.displayName());
        history.setActorRole(CurrentUser.primaryRole());
        return history;
    }

    private void notifyClosure(WorkOrder workOrder) {
        var context = new NotificationCopy.WorkOrderContext(
                workOrder.getCode(),
                workOrder.getSummary(),
                workOrder.getCustomer() == null ? null : workOrder.getCustomer().getName()
        );
        String actorLabel = "Chăm sóc khách hàng " + CurrentUser.displayName();
        var ownerCopy = NotificationCopy.workOrderClosedForOwner(context, actorLabel);
        notificationService.notifyRoles(
                workOrder.getTenantId(),
                List.of(UserRole.OWNER),
                ownerCopy.title(),
                ownerCopy.message()
        );
        if (workOrder.getTechnician() != null && workOrder.getTechnician().getUser() != null) {
            var technicianCopy = NotificationCopy.workOrderClosedForTechnician(context, actorLabel);
            notificationService.create(
                    workOrder.getTenantId(),
                    workOrder.getTechnician().getUser(),
                    technicianCopy.title(),
                    technicianCopy.message()
            );
        }
    }

    private static void requireCustomerServiceRole() {
        if (!CurrentUser.hasRole("CUSTOMER_SERVICE")) {
            throw BusinessException.forbidden(
                    "WORK_ORDER_CLOSE_DENIED",
                    "Chỉ chăm sóc khách hàng được đóng phiếu sau khi đối soát thanh toán"
            );
        }
    }
}
