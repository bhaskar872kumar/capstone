package com.healthdekho.recommendation;

import com.healthdekho.common.error.BadRequestException;
import com.healthdekho.policy.PolicyCalculator;
import com.healthdekho.product.InsuranceProduct;
import com.healthdekho.product.ProductService;
import com.healthdekho.recommendation.dto.RecommendationItemResponse;
import com.healthdekho.recommendation.dto.RecommendationRequest;
import com.healthdekho.recommendation.dto.RecommendationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * FR-2: AI insurance recommendations.
 * Only active products the customer doesn't hold and can join at their age are offered to the model,
 * every answer is checked against that list, and a rule-based ranking is used if the model fails.
 * AI never decides eligibility or prices.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final int MAX_REASON_LENGTH = 300;

    enum Source { AI, FALLBACK, NONE }

    private final ProductService productService;
    private final RecommendationAiClient aiClient;
    private final AiCallExecutor aiCallExecutor;
    private final FallbackRanker fallbackRanker;
    private final RecommendationRecorder recorder;
    private final AiProperties properties;
    private final Clock clock;

    public RecommendationService(ProductService productService,
                                 RecommendationAiClient aiClient,
                                 AiCallExecutor aiCallExecutor,
                                 FallbackRanker fallbackRanker,
                                 RecommendationRecorder recorder,
                                 AiProperties properties,
                                 Clock clock) {
        this.productService = productService;
        this.aiClient = aiClient;
        this.aiCallExecutor = aiCallExecutor;
        this.fallbackRanker = fallbackRanker;
        this.recorder = recorder;
        this.properties = properties;
        this.clock = clock;
    }

    // Deliberately not @Transactional: the AI call must not hold a database transaction open.
    public RecommendationResponse recommend(Long customerId, RecommendationRequest request) {
        long started = System.nanoTime();
        LocalDate today = LocalDate.now(clock);
        if (request.dateOfBirth().isAfter(today)) {
            throw new BadRequestException("INVALID_DATE_OF_BIRTH", "Date of birth can't be in the future.");
        }
        int age = PolicyCalculator.ageOn(request.dateOfBirth(), today);

        List<InsuranceProduct> candidates = productService.availableProducts(customerId).stream()
                .filter(product -> PolicyCalculator.isEligible(age, product.getMinEntryAge(), product.getMaxEntryAge()))
                .toList();

        Outcome outcome = candidates.isEmpty()
                ? new Outcome(Source.NONE, List.of())
                : rank(age, request, candidates);

        boolean fromAi = outcome.source() == Source.AI;
        RecommendationRecorder.SavedRecommendation saved = recorder.save(
                customerId, request, age,
                fromAi ? properties.modelName() : "fallback",
                fromAi ? properties.modelVersion() : "rule-based-v1",
                properties.promptVersion(),
                outcome.items());

        log.info("Recommendation requestId={} customerId={} source={} items={} tookMs={}",
                saved.requestId(), customerId, outcome.source(), outcome.items().size(),
                (System.nanoTime() - started) / 1_000_000);

        List<RecommendationItemResponse> items = IntStream.range(0, outcome.items().size())
                .mapToObj(index -> RecommendationItemResponse.from(saved.itemIds().get(index), outcome.items().get(index)))
                .toList();
        return new RecommendationResponse(saved.requestId(), age, outcome.source().name(),
                messageFor(outcome.source()), items);
    }

    private Outcome rank(int age, RecommendationRequest request, List<InsuranceProduct> candidates) {
        Map<String, InsuranceProduct> byCode = candidates.stream()
                .collect(Collectors.toMap(InsuranceProduct::getProductCode, Function.identity()));
        RecommendationPrompt prompt = new RecommendationPrompt(
                age,
                request.insuranceGoals().trim(),
                request.preferredTermYears(),
                request.preferredPolicyType(),
                candidates.stream().map(CandidateProduct::from).toList(),
                properties.maxItems());

        List<RankedSuggestion> fromAi = askAi(prompt)
                .map(response -> checkAgainstDatabase(response, byCode))
                .orElse(List.of());
        if (!fromAi.isEmpty()) {
            return new Outcome(Source.AI, fromAi);
        }
        return new Outcome(Source.FALLBACK, fallbackRanker.rank(
                candidates, request.preferredTermYears(), request.preferredPolicyType(), properties.maxItems()));
    }

    private Optional<RecommendationAiResponse> askAi(RecommendationPrompt prompt) {
        try {
            Optional<RecommendationAiResponse> response = aiCallExecutor.call(() -> aiClient.suggest(prompt),
                    properties.timeout());
            return response == null ? Optional.empty() : response;
        } catch (TimeoutException ex) {
            log.warn("AI recommendation timed out after {} ms; using fallback", properties.timeout().toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("AI recommendation was interrupted; using fallback");
        } catch (ExecutionException | RuntimeException ex) {
            Throwable cause = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
            log.warn("AI recommendation failed ({}); using fallback", cause.getClass().getSimpleName());
        }
        return Optional.empty();
    }

    /** Drops anything the model invented or repeated; keeps the model's order; re-numbers ranks 1..n. */
    List<RankedSuggestion> checkAgainstDatabase(RecommendationAiResponse response, Map<String, InsuranceProduct> byCode) {
        if (response == null || response.items() == null) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<RecommendationAiResponse.Suggestion> valid = response.items().stream()
                .filter(Objects::nonNull)
                .filter(suggestion -> suggestion.productCode() != null
                        && byCode.containsKey(suggestion.productCode().trim()))
                .sorted(Comparator.comparing(
                        (RecommendationAiResponse.Suggestion suggestion) ->
                                suggestion.rank() == null ? Integer.MAX_VALUE : suggestion.rank()))
                .filter(suggestion -> seen.add(suggestion.productCode().trim()))
                .limit(properties.maxItems())
                .toList();

        return IntStream.range(0, valid.size())
                .mapToObj(index -> {
                    RecommendationAiResponse.Suggestion suggestion = valid.get(index);
                    return new RankedSuggestion(
                            byCode.get(suggestion.productCode().trim()),
                            index + 1,
                            cleanReason(suggestion.reason()),
                            Suitability.parse(suggestion.suitability()));
                })
                .toList();
    }

    private static String cleanReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Fits the goals you described.";
        }
        String cleaned = reason.replaceAll("\\s+", " ").trim();
        return cleaned.length() > MAX_REASON_LENGTH ? cleaned.substring(0, MAX_REASON_LENGTH) : cleaned;
    }

    private static String messageFor(Source source) {
        return switch (source) {
            case AI -> "Here are the policies that best match your goals.";
            case FALLBACK -> "AI suggestions aren't available right now, so these are ranked by our standard rules.";
            case NONE -> "There are no new policies open to your age right now.";
        };
    }

    private record Outcome(Source source, List<RankedSuggestion> items) {
    }
}
