package com.healthdekho.policy.dto;

import com.healthdekho.product.PolicyType;
import com.healthdekho.product.PremiumFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;

/** FR-3 A: everything the registration form shows, calculated by the server. */
public record QuoteResponse(
        String customerName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        String productCode,
        PolicyType policyType,
        String policyName,
        int termMonths,
        String policyTerm,
        LocalDate policyStartDate,
        LocalDate policyMaturityDate,
        PremiumFrequency premiumFrequency,
        BigDecimal premiumAmount,
        BigDecimal sumInsured,
        BigDecimal totalPremiumPayable,
        int installmentCount,
        int ageOnStartDate,
        int minEntryAge,
        int maxEntryAge,
        boolean eligible,
        String ineligibleReason) {
}
