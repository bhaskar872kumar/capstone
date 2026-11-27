package com.healthdekho.recommendation.dto;

import com.healthdekho.common.TermLabel;
import com.healthdekho.product.InsuranceProduct;
import com.healthdekho.product.PolicyType;
import com.healthdekho.product.PremiumFrequency;
import com.healthdekho.recommendation.RankedSuggestion;
import com.healthdekho.recommendation.Suitability;

import java.math.BigDecimal;

/** FR-2 D table row. */
public record RecommendationItemResponse(
        Long recommendationItemId,
        int rank,
        String productCode,
        String policyName,
        PolicyType policyType,
        int termMonths,
        String policyTerm,
        BigDecimal sumInsured,
        PremiumFrequency premiumFrequency,
        BigDecimal premiumAmount,
        String recommendationReason,
        Suitability suitability) {

    public static RecommendationItemResponse from(Long itemId, RankedSuggestion suggestion) {
        InsuranceProduct product = suggestion.product();
        return new RecommendationItemResponse(
                itemId,
                suggestion.rank(),
                product.getProductCode(),
                product.getPolicyName(),
                product.getPolicyType(),
                product.getTermMonths(),
                TermLabel.of(product.getTermMonths()),
                product.getSumInsured(),
                product.getPremiumFrequency(),
                product.getPremiumAmount(),
                suggestion.reason(),
                suggestion.suitability());
    }
}
