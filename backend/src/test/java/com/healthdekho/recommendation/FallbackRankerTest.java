package com.healthdekho.recommendation;

import com.healthdekho.product.InsuranceProduct;
import com.healthdekho.product.PolicyType;
import com.healthdekho.product.PremiumFrequency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** AI_08: the rule-based fallback ranking is deterministic and explainable. */
class FallbackRankerTest {

    private final FallbackRanker ranker = new FallbackRanker();

    private static InsuranceProduct product(String code, PolicyType type, int term, PremiumFrequency frequency,
                                            String premium, String sumInsured) {
        return new InsuranceProduct(code, code, type, term, frequency, new BigDecimal(premium),
                new BigDecimal(sumInsured), 18, 60, true);
    }

    @Test
    void AI_08_preferredTypeAndTermComeFirst() {
        InsuranceProduct term = product("TERM", PolicyType.TERM, 240, PremiumFrequency.MONTHLY, "1250.00", "5000000.00");
        InsuranceProduct health = product("HEALTH", PolicyType.HEALTH, 12, PremiumFrequency.YEARLY, "18000.00", "1000000.00");
        InsuranceProduct savings = product("SAV", PolicyType.SAVINGS, 120, PremiumFrequency.QUARTERLY, "7500.00", "1500000.00");

        List<RankedSuggestion> ranked = ranker.rank(List.of(health, savings, term), 20, PolicyType.TERM, 3);

        assertThat(ranked).extracting(suggestion -> suggestion.product().getProductCode())
                .containsExactly("TERM", "HEALTH", "SAV");
        assertThat(ranked.getFirst().suitability()).isEqualTo(Suitability.HIGH);
        assertThat(ranked.getFirst().reason()).startsWith("Rule-based suggestion:").contains("term cover");
        assertThat(ranked).extracting(RankedSuggestion::rank).containsExactly(1, 2, 3);
    }

    @Test
    void AI_09_limitsResultsAndHandlesEmptyList() {
        InsuranceProduct a = product("A", PolicyType.HEALTH, 12, PremiumFrequency.YEARLY, "1000.00", "100000.00");
        InsuranceProduct b = product("B", PolicyType.HEALTH, 12, PremiumFrequency.YEARLY, "2000.00", "100000.00");

        assertThat(ranker.rank(List.of(a, b), null, null, 1)).hasSize(1);
        assertThat(ranker.rank(List.of(), 10, PolicyType.HEALTH, 3)).isEmpty();
    }

    @Test
    void AI_10_suitabilityBands() {
        assertThat(FallbackRanker.suitabilityFor(6)).isEqualTo(Suitability.HIGH);
        assertThat(FallbackRanker.suitabilityFor(2)).isEqualTo(Suitability.MEDIUM);
        assertThat(FallbackRanker.suitabilityFor(0)).isEqualTo(Suitability.LOW);
    }
}
