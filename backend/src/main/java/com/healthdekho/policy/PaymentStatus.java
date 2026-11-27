package com.healthdekho.policy;

/** Installment payment status (spec 5.2 premium_installment.payment_status). New installments start as PENDING. */
public enum PaymentStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    OVERDUE
}
