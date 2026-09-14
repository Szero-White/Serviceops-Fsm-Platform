package com.serviceops.workorder.application;

import com.serviceops.payment.domain.Payment;
import com.serviceops.payment.domain.PaymentMethod;
import com.serviceops.payment.domain.PaymentReceipt;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityResponse;
import com.serviceops.workorder.web.WorkOrderDtos.WorkOrderActivityType;

import java.time.Instant;
import java.util.List;

final class WorkOrderPaymentActivityMapper {
    private WorkOrderPaymentActivityMapper() {
    }

    static void addPaymentActivities(List<WorkOrderActivityResponse> activities, Payment payment) {
        if (payment == null) {
            return;
        }
        if (payment.getTransferReportedAt() != null) {
            String username = assignedTechnicianUsername(payment);
            String displayName = assignedTechnicianDisplayName(payment);
            activities.add(paymentActivity(
                    "payment:" + payment.getId() + ":reported",
                    WorkOrderActivityType.PAYMENT_REPORTED,
                    "Khách báo đã chuyển khoản",
                    username,
                    displayName,
                    "TECHNICIAN",
                    payment,
                    payment.getTransferReportedAt()
            ));
        } else if (payment.getCashCollectedAt() != null) {
            activities.add(paymentActivity(
                    "payment:" + payment.getId() + ":reported",
                    WorkOrderActivityType.PAYMENT_REPORTED,
                    "Đã nhận tiền mặt từ khách; chờ bàn giao về công ty",
                    payment.getCollectedByUsername(),
                    payment.getCollectedByDisplayName(),
                    "TECHNICIAN",
                    payment,
                    payment.getCashCollectedAt()
            ));
        } else if (payment.getCounterPaymentRequestedAt() != null) {
            activities.add(paymentActivity(
                    "payment:" + payment.getId() + ":reported",
                    WorkOrderActivityType.PAYMENT_REPORTED,
                    "Khách hẹn thanh toán trực tiếp tại quầy chăm sóc khách hàng",
                    assignedTechnicianUsername(payment),
                    assignedTechnicianDisplayName(payment),
                    "TECHNICIAN",
                    payment,
                    payment.getCounterPaymentRequestedAt()
            ));
        }
        if (payment.getSettledAt() != null) {
            activities.add(paymentActivity(
                    "payment:" + payment.getId() + ":settled",
                    WorkOrderActivityType.PAYMENT_SETTLED,
                    "Bộ phận chăm sóc khách hàng đã đối soát tiền về công ty",
                    payment.getSettledByUsername(),
                    payment.getSettledByDisplayName(),
                    "CUSTOMER_SERVICE",
                    payment,
                    payment.getSettledAt()
            ));
        }
    }

    static WorkOrderActivityResponse fromReceipt(PaymentReceipt receipt) {
        return WorkOrderActivityFactory.activity(
                "receipt:" + receipt.getId(),
                WorkOrderActivityType.RECEIPT_ISSUED,
                null,
                "Biên nhận thanh toán dịch vụ đã được phát hành",
                receipt.getIssuedByUsername(),
                receipt.getIssuedByDisplayName(),
                "CUSTOMER_SERVICE",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                receipt.getAmount(),
                paymentMethod(receipt.getPaymentMethod()),
                receipt.getReceiptCode(),
                receipt.getIssuedAt()
        );
    }

    private static WorkOrderActivityResponse paymentActivity(
            String id,
            WorkOrderActivityType type,
            String note,
            String actor,
            String actorDisplayName,
            String actorRole,
            Payment payment,
            Instant createdAt
    ) {
        return WorkOrderActivityFactory.activity(
                id,
                type,
                null,
                note,
                actor,
                actorDisplayName,
                actorRole,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                payment.getAmount(),
                paymentMethod(payment.getMethod()),
                null,
                createdAt
        );
    }

    private static String assignedTechnicianUsername(Payment payment) {
        return payment.getWorkOrder().getTechnician() == null
                || payment.getWorkOrder().getTechnician().getUser() == null
                ? null
                : payment.getWorkOrder().getTechnician().getUser().getUsername();
    }

    private static String assignedTechnicianDisplayName(Payment payment) {
        return payment.getWorkOrder().getTechnician() == null
                || payment.getWorkOrder().getTechnician().getUser() == null
                ? null
                : payment.getWorkOrder().getTechnician().getUser().getDisplayName();
    }

    private static String paymentMethod(PaymentMethod method) {
        return method == null ? null : method.name();
    }
}
