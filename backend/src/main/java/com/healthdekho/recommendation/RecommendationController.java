package com.healthdekho.recommendation;

import com.healthdekho.recommendation.dto.RecommendationRequest;
import com.healthdekho.recommendation.dto.RecommendationResponse;
import com.healthdekho.security.CurrentCustomer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FR-2 D: generate personalised recommendations from the dashboard. */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final CurrentCustomer currentCustomer;

    public RecommendationController(RecommendationService recommendationService, CurrentCustomer currentCustomer) {
        this.recommendationService = recommendationService;
        this.currentCustomer = currentCustomer;
    }

    @PostMapping
    public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request) {
        return recommendationService.recommend(currentCustomer.id(), request);
    }
}
