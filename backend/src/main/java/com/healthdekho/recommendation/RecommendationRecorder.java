package com.healthdekho.recommendation;

import com.healthdekho.customer.CustomerRepository;
import com.healthdekho.product.InsuranceProductRepository;
import com.healthdekho.recommendation.dto.RecommendationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/** Saves a recommendation request and its items in one transaction (spec: save recommendations against the customer). */
@Service
public class RecommendationRecorder {

    private final AiRecommendationRequestRepository requestRepository;
    private final AiRecommendationItemRepository itemRepository;
    private final CustomerRepository customerRepository;
    private final InsuranceProductRepository productRepository;
    private final Clock clock;

    public RecommendationRecorder(AiRecommendationRequestRepository requestRepository,
                                  AiRecommendationItemRepository itemRepository,
                                  CustomerRepository customerRepository,
                                  InsuranceProductRepository productRepository,
                                  Clock clock) {
        this.requestRepository = requestRepository;
        this.itemRepository = itemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.clock = clock;
    }

    @Transactional
    public SavedRecommendation save(Long customerId, RecommendationRequest request, int calculatedAge,
                                    String modelName, String modelVersion, String promptVersion,
                                    List<RankedSuggestion> suggestions) {
        String preferredTerm = request.preferredTermYears() == null
                ? null
                : request.preferredTermYears() + (request.preferredTermYears() == 1 ? " year" : " years");

        AiRecommendationRequest saved = requestRepository.save(new AiRecommendationRequest(
                customerRepository.getReferenceById(customerId),
                request.insuranceGoals().trim(),
                preferredTerm,
                request.preferredPolicyType(),
                calculatedAge,
                modelName,
                modelVersion,
                promptVersion,
                OffsetDateTime.now(clock)));

        List<AiRecommendationItem> items = suggestions.stream()
                .map(suggestion -> new AiRecommendationItem(
                        saved,
                        productRepository.getReferenceById(suggestion.product().getId()),
                        suggestion.rank(),
                        suggestion.reason(),
                        suggestion.suitability()))
                .toList();
        List<Long> itemIds = itemRepository.saveAll(items).stream()
                .map(AiRecommendationItem::getId)
                .toList();

        return new SavedRecommendation(saved.getId(), itemIds);
    }

    public record SavedRecommendation(Long requestId, List<Long> itemIds) {
    }
}
