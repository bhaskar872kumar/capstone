package com.healthdekho.recommendation;

import com.healthdekho.common.error.BadRequestException;
import com.healthdekho.product.InsuranceProduct;
import com.healthdekho.product.PolicyType;
import com.healthdekho.product.PremiumFrequency;
import com.healthdekho.product.ProductService;
import com.healthdekho.recommendation.dto.RecommendationRequest;
import com.healthdekho.recommendation.dto.RecommendationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US-04 AI recommendations: AI contract, validation against the database, fallback. */
class RecommendationServiceTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final Long CUSTOMER_ID = 1001L;

    private final ProductService productService = mock(ProductService.class);
    private final RecommendationAiClient aiClient = mock(RecommendationAiClient.class);
    private final RecommendationRecorder recorder = mock(RecommendationRecorder.class);
    private final AiCallExecutor executor = new AiCallExecutor();

    private InsuranceProduct health;
    private InsuranceProduct savings;
    private InsuranceProduct youth;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        health = product(202L, "HD-HEALTH-001", PolicyType.HEALTH, 12, PremiumFrequency.YEARLY, "18000.00", "1000000.00", 18, 65);
        savings = product(203L, "HD-SAV-001", PolicyType.SAVINGS, 120, PremiumFrequency.QUARTERLY, "7500.00", "1500000.00", 21, 55);
        youth = product(299L, "HD-YOUTH-TEST", PolicyType.HEALTH, 12, PremiumFrequency.YEARLY, "6000.00", "300000.00", 18, 25);
        when(productService.availableProducts(CUSTOMER_ID)).thenReturn(List.of(health, savings, youth));
        when(recorder.save(anyLong(), any(), anyInt(), anyString(), anyString(), anyString(), anyList()))
                .thenAnswer(call -> {
                    List<?> items = call.getArgument(6);
                    return new RecommendationRecorder.SavedRecommendation(9001L,
                            IntStream.range(0, items.size()).mapToObj(i -> 9100L + i).toList());
                });
        service = serviceWithTimeout(Duration.ofSeconds(2));
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    private RecommendationService serviceWithTimeout(Duration timeout) {
        Clock clock = Clock.fixed(LocalDate.of(2026, 11, 25).atTime(10, 0).atZone(IST).toInstant(), IST);
        AiProperties properties = new AiProperties("amazon.nova-lite-v1:0", "bedrock-converse", "rec-v1", timeout, 3);
        return new RecommendationService(productService, aiClient, executor, new FallbackRanker(), recorder, properties, clock);
    }

    private static RecommendationRequest request() {
        return new RecommendationRequest(LocalDate.of(1990, 4, 15), "Family health cover and some savings", 10, PolicyType.HEALTH);
    }

    private static RecommendationAiResponse.Suggestion suggestion(String code, int rank, String suitability) {
        return new RecommendationAiResponse.Suggestion(code, rank, "Because it fits your goals.", suitability);
    }

    @Test
    void AI_01_validAiAnswerIsReturnedAndSaved() {
        when(aiClient.suggest(any())).thenReturn(Optional.of(new RecommendationAiResponse(List.of(
                suggestion("HD-HEALTH-001", 1, "HIGH"),
                suggestion("HD-SAV-001", 2, "medium")))));

        RecommendationResponse response = service.recommend(CUSTOMER_ID, request());

        assertThat(response.source()).isEqualTo("AI");
        assertThat(response.calculatedAge()).isEqualTo(36);
        assertThat(response.requestId()).isEqualTo(9001L);
        assertThat(response.items()).extracting("productCode").containsExactly("HD-HEALTH-001", "HD-SAV-001");
        assertThat(response.items()).extracting("suitability").containsExactly(Suitability.HIGH, Suitability.MEDIUM);
        assertThat(response.items().getFirst().recommendationItemId()).isEqualTo(9100L);
        verify(recorder).save(eq(CUSTOMER_ID), any(), eq(36), eq("amazon.nova-lite-v1:0"), eq("bedrock-converse"),
                eq("rec-v1"), anyList());
    }

    @Test
    void AI_02_productsTheModelInventedAreDropped() {
        when(aiClient.suggest(any())).thenReturn(Optional.of(new RecommendationAiResponse(List.of(
                suggestion("HD-FAKE-999", 1, "HIGH"),
                suggestion("HD-SAV-001", 2, "HIGH"),
                suggestion("HD-SAV-001", 3, "LOW")))));

        RecommendationResponse response = service.recommend(CUSTOMER_ID, request());

        assertThat(response.source()).isEqualTo("AI");
        assertThat(response.items()).extracting("productCode").containsExactly("HD-SAV-001");
        assertThat(response.items().getFirst().rank()).isEqualTo(1);
    }

    @Test
    void AI_03_timeoutUsesRuleBasedFallback() {
        when(aiClient.suggest(any())).thenAnswer(call -> {
            Thread.sleep(1_000);
            return Optional.empty();
        });

        RecommendationResponse response = serviceWithTimeout(Duration.ofMillis(50)).recommend(CUSTOMER_ID, request());

        assertThat(response.source()).isEqualTo("FALLBACK");
        assertThat(response.items()).isNotEmpty();
        assertThat(response.items().getFirst().productCode()).isEqualTo("HD-HEALTH-001");
        assertThat(response.items().getFirst().recommendationReason()).startsWith("Rule-based suggestion:");
        verify(recorder).save(eq(CUSTOMER_ID), any(), eq(36), eq("fallback"), eq("rule-based-v1"), eq("rec-v1"), anyList());
    }

    @Test
    void AI_04_ageIneligibleProductsAreNeverSentOrRecommended() {
        when(aiClient.suggest(any())).thenReturn(Optional.of(new RecommendationAiResponse(List.of(
                suggestion("HD-YOUTH-TEST", 1, "HIGH")))));

        RecommendationResponse response = service.recommend(CUSTOMER_ID, request());

        ArgumentCaptor<RecommendationPrompt> prompt = ArgumentCaptor.forClass(RecommendationPrompt.class);
        verify(aiClient).suggest(prompt.capture());
        assertThat(prompt.getValue().candidates()).extracting("productCode")
                .containsExactlyInAnyOrder("HD-HEALTH-001", "HD-SAV-001");
        // The only AI suggestion was not allowed, so the fallback ranking is used instead.
        assertThat(response.source()).isEqualTo("FALLBACK");
        assertThat(response.items()).extracting("productCode").doesNotContain("HD-YOUTH-TEST");
    }

    @Test
    void AI_05_futureDateOfBirthIsRejected() {
        RecommendationRequest future = new RecommendationRequest(LocalDate.of(2027, 1, 1), "Health cover", null, null);
        assertThatThrownBy(() -> service.recommend(CUSTOMER_ID, future)).isInstanceOf(BadRequestException.class);
        verify(aiClient, never()).suggest(any());
    }

    @Test
    void AI_06_aiErrorUsesFallback() {
        when(aiClient.suggest(any())).thenThrow(new IllegalStateException("model unavailable"));

        RecommendationResponse response = service.recommend(CUSTOMER_ID, request());

        assertThat(response.source()).isEqualTo("FALLBACK");
        assertThat(response.items()).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void AI_07_noOpenProductsGivesEmptyResultWithoutCallingAi() {
        when(productService.availableProducts(CUSTOMER_ID)).thenReturn(List.of(youth));

        RecommendationResponse response = service.recommend(CUSTOMER_ID, request());

        assertThat(response.source()).isEqualTo("NONE");
        assertThat(response.items()).isEmpty();
        verify(aiClient, never()).suggest(any());
    }

    private static InsuranceProduct product(Long id, String code, PolicyType type, int term, PremiumFrequency frequency,
                                            String premium, String sumInsured, int minAge, int maxAge) {
        InsuranceProduct product = new InsuranceProduct(code, code + " plan", type, term, frequency,
                new BigDecimal(premium), new BigDecimal(sumInsured), minAge, maxAge, true);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
