package com.healthdekho.recommendation;

import com.healthdekho.product.InsuranceProduct;
import com.healthdekho.product.PolicyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Safe fallback when the AI model times out, fails or returns nothing usable:
 * ranks the same eligible products with simple, explainable rules.
 */
@Component
public class FallbackRanker {

    public List<RankedSuggestion> rank(List<InsuranceProduct> candidates, Integer preferredTermYears,
                                       PolicyType preferredType, int maxItems) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        BigDecimal bestCostRatio = candidates.stream()
                .map(FallbackRanker::costRatio)
                .min(Comparator.naturalOrder())
                .orElseThrow();

        List<Scored> ranked = candidates.stream()
                .map(product -> score(product, preferredTermYears, preferredType, bestCostRatio))
                .sorted(Comparator.comparingInt(Scored::score).reversed()
                        .thenComparing(scored -> annualPremium(scored.product())))
                .limit(maxItems)
                .toList();

        return IntStream.range(0, ranked.size())
                .mapToObj(index -> {
                    Scored scored = ranked.get(index);
                    String reason = "Rule-based suggestion: " + (scored.reasons().isEmpty()
                            ? "open to your age group"
                            : String.join(", ", scored.reasons())) + ".";
                    return new RankedSuggestion(scored.product(), index + 1, reason, suitabilityFor(scored.score()));
                })
                .toList();
    }

    private static Scored score(InsuranceProduct product, Integer preferredTermYears, PolicyType preferredType,
                                BigDecimal bestCostRatio) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (preferredType != null && product.getPolicyType() == preferredType) {
            score += 3;
            reasons.add("matches the %s cover you asked for".formatted(preferredType.name().toLowerCase(Locale.ROOT)));
        }
        if (preferredTermYears != null) {
            double difference = Math.abs(product.getTermMonths() / 12.0 - preferredTermYears);
            if (difference <= 2) {
                score += 2;
                reasons.add("term is close to the %d years you prefer".formatted(preferredTermYears));
            } else if (difference <= 5) {
                score += 1;
                reasons.add("term is near the length you prefer");
            }
        }
        if (costRatio(product).compareTo(bestCostRatio) == 0) {
            score += 1;
            reasons.add("lowest yearly premium for the cover among your options");
        }
        return new Scored(product, score, reasons);
    }

    static Suitability suitabilityFor(int score) {
        if (score >= 4) {
            return Suitability.HIGH;
        }
        return score >= 2 ? Suitability.MEDIUM : Suitability.LOW;
    }

    static BigDecimal annualPremium(InsuranceProduct product) {
        return product.getPremiumAmount()
                .multiply(BigDecimal.valueOf(product.getPremiumFrequency().installmentsPerYear()));
    }

    static BigDecimal costRatio(InsuranceProduct product) {
        return annualPremium(product).divide(product.getSumInsured(), 10, RoundingMode.HALF_UP);
    }

    private record Scored(InsuranceProduct product, int score, List<String> reasons) {
    }
}
