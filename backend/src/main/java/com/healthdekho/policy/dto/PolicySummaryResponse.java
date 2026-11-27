package com.healthdekho.policy.dto;

import com.healthdekho.common.TermLabel;
import com.healthdekho.policy.CustomerPolicy;
import com.healthdekho.policy.PolicyStatus;
import com.healthdekho.product.PolicyType;
import com.healthdekho.product.PremiumFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;

/** FR-1 B dashboard row. */
public record PolicySummaryResponse(
        String policyNumber,
        String policyName,
        PolicyType policyType,
        int termMonths,
        String policyTerm,
        LocalDate policyStartDate,
        LocalDate policyMaturityDate,
        PremiumFrequency premiumFrequency,
        BigDecimal premiumAmount,
        BigDecimal sumInsured,
        PolicyStatus policyStatus) {

    public static PolicySummaryResponse from(CustomerPolicy policy) {
        int termMonths = policy.getProduct().getTermMonths();
        return new PolicySummaryResponse(
                policy.getPolicyNumber(),
                policy.getProduct().getPolicyName(),
                policy.getProduct().getPolicyType(),
                termMonths,
                TermLabel.of(termMonths),
                policy.getStartDate(),
                policy.getMaturityDate(),
                policy.getPremiumFrequency(),
                policy.getPremiumAmount(),
                policy.getSumInsured(),
                policy.getStatus());
    }
}
