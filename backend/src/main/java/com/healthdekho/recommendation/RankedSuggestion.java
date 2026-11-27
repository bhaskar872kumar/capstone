package com.healthdekho.recommendation;

import com.healthdekho.product.InsuranceProduct;

/** A checked recommendation: always a real product from the database. */
public record RankedSuggestion(InsuranceProduct product, int rank, String reason, Suitability suitability) {
}
