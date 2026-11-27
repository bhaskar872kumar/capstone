package com.healthdekho.recommendation;

import com.healthdekho.product.InsuranceProduct;
import com.healthdekho.product.PolicyType;
import com.healthdekho.product.PremiumFrequency;

import java.math.BigDecimal;

/** The product facts sent to the AI model. Only products from the database are ever sent. */
public record CandidateProduct(
        String productCode,
        String policyName,
        PolicyType policyType,
        int termMonths,
        PremiumFrequency premiumFrequency,
        BigDecimal premiumAmount,
        BigDecimal sumInsured) {

    public static CandidateProduct from(InsuranceProduct product) {
        return new CandidateProduct(
                product.getProductCode(),
                product.getPolicyName(),
                product.getPolicyType(),
                product.getTermMonths(),
                product.getPremiumFrequency(),
                product.getPremiumAmount(),
                product.getSumInsured());
    }
}
