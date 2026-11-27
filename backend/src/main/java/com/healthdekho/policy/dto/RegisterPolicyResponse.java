package com.healthdekho.policy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterPolicyResponse(
        String policyNumber,
        String message,
        LocalDate policyStartDate,
        LocalDate policyMaturityDate,
        BigDecimal totalPremiumPayable,
        int installmentCount,
        LocalDate firstDueDate) {
}
