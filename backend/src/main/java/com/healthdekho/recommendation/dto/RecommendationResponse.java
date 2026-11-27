package com.healthdekho.recommendation.dto;

import java.util.List;

/**
 * FR-2 result. source is AI when the model's answer was used, FALLBACK when rule-based ranking was used,
 * NONE when no product is open to the customer.
 */
public record RecommendationResponse(
        Long requestId,
        int calculatedAge,
        String source,
        String message,
        List<RecommendationItemResponse> items) {
}
