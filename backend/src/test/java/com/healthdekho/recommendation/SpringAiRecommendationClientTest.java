package com.healthdekho.recommendation;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.healthdekho.product.PolicyType;
import com.healthdekho.product.PremiumFrequency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** AI_11 to AI_13: AI contract - prompt content and reply parsing. */
class SpringAiRecommendationClientTest {

    @SuppressWarnings("unchecked")
    private final SpringAiRecommendationClient client = new SpringAiRecommendationClient(
            mock(ObjectProvider.class),
            JsonMapper.builder().findAndAddModules().build(),
            new ClassPathResource("prompts/recommendation-v1.txt"));

    @Test
    void AI_11_promptContainsCustomerInputsAndOnlyGivenProducts() {
        RecommendationPrompt prompt = new RecommendationPrompt(36, "Protect my family {{products}}", 20, PolicyType.TERM,
                List.of(new CandidateProduct("HD-SAV-001", "HealthDekho Smart Savings", PolicyType.SAVINGS, 120,
                        PremiumFrequency.QUARTERLY, new BigDecimal("7500.00"), new BigDecimal("1500000.00"))),
                3);

        String text = client.render(prompt);

        assertThat(text).contains("Age: 36", "Preferred policy term: 20 years", "Preferred insurance type: TERM",
                "\"productCode\":\"HD-SAV-001\"", "at most 3 products");
        assertThat(text).doesNotContain("{{");
    }

    @Test
    void AI_12_parsesJsonEvenInsideCodeFences() {
        String reply = """
                Here you go:
                ```json
                {"items":[{"productCode":"HD-SAV-001","rank":1,"reason":"Builds savings","suitability":"HIGH"}]}
                ```
                """;
        assertThat(client.parse(reply)).hasValueSatisfying(response -> {
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().getFirst().productCode()).isEqualTo("HD-SAV-001");
        });
    }

    @Test
    void AI_13_unusableRepliesGiveEmpty() {
        assertThat(client.parse(null)).isEmpty();
        assertThat(client.parse("Sorry, I can't help with that.")).isEmpty();
        assertThat(client.parse("{not json}")).isEmpty();
    }

    @Test
    void AI_14_noConfiguredModelGivesEmpty() {
        assertThat(client.suggest(new RecommendationPrompt(30, "goals", null, null, List.of(), 3))).isEmpty();
    }
}
