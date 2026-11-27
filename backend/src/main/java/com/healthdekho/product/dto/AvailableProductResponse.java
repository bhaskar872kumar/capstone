package com.healthdekho.product.dto;

import com.healthdekho.common.TermLabel;
import com.healthdekho.product.InsuranceProduct;
import com.healthdekho.product.PolicyType;
import com.healthdekho.product.PremiumFrequency;

import java.math.BigDecimal;

/** FR-1 C row: an active product the customer hasn't registered yet. */
public record AvailableProductResponse(
        String productCode,
        String policyName,
        PolicyType policyType,
        int termMonths,
        String policyTerm,
        BigDecimal sumInsured,
        PremiumFrequency premiumFrequency,
        BigDecimal premiumAmount,
        int minEntryAge,
        int maxEntryAge) {

    public static AvailableProductResponse from(InsuranceProduct product) {
        return new AvailableProductResponse(
                product.getProductCode(),
                product.getPolicyName(),
                product.getPolicyType(),
                product.getTermMonths(),
                TermLabel.of(product.getTermMonths()),
                product.getSumInsured(),
                product.getPremiumFrequency(),
                product.getPremiumAmount(),
                product.getMinEntryAge(),
                product.getMaxEntryAge());
    }
}
