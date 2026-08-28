package com.serviceops.payment.domain;

public enum PaymentStatus {
    UNPAID,
    TRANSFER_PENDING_VERIFICATION,
    CASH_PENDING_HANDOVER,
    SETTLED
}
