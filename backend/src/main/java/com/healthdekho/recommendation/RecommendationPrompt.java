package com.healthdekho.recommendation;

import com.healthdekho.product.PolicyType;

import java.util.List;

/** Everything the AI model needs for one recommendation. */
public record RecommendationPrompt(
        int age,
        String insuranceGoals,
        Integer preferredTermYears,
        PolicyType preferredPolicyType,
        List<CandidateProduct> candidates,
        int maxItems) {
}
