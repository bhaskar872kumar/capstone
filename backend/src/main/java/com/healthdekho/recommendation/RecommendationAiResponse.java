package com.healthdekho.recommendation;

import java.util.List;

/** The JSON shape the model is asked to return. Nothing in it is trusted until it's checked. */
public record RecommendationAiResponse(List<Suggestion> items) {

    public record Suggestion(String productCode, Integer rank, String reason, String suitability) {
    }
}
