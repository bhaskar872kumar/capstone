package com.healthdekho.recommendation;

import java.util.Optional;

/**
 * AI integration layer (spec 2.3). Returns the model's raw suggestions, or empty when no answer could be used.
 * The service checks every suggestion against the database and falls back to rule-based ranking.
 */
public interface RecommendationAiClient {

    Optional<RecommendationAiResponse> suggest(RecommendationPrompt prompt);
}
