package com.healthdekho.recommendation;

import com.healthdekho.product.InsuranceProduct;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A product returned for a recommendation request (spec 5.2 ai_recommendation_item). */
@Entity
@Table(name = "ai_recommendation_item")
public class AiRecommendationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_request_id", nullable = false)
    private AiRecommendationRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct product;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(name = "recommendation_reason", nullable = false, length = 500)
    private String recommendationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "suitability", nullable = false, length = 10)
    private Suitability suitability;

    @Column(name = "selected_for_registration", nullable = false)
    private boolean selectedForRegistration;

    protected AiRecommendationItem() {
    }

    public AiRecommendationItem(AiRecommendationRequest request, InsuranceProduct product, int rankOrder,
                                String recommendationReason, Suitability suitability) {
        this.request = request;
        this.product = product;
        this.rankOrder = rankOrder;
        this.recommendationReason = recommendationReason;
        this.suitability = suitability;
    }

    public Long getId() {
        return id;
    }

    public AiRecommendationRequest getRequest() {
        return request;
    }

    public InsuranceProduct getProduct() {
        return product;
    }

    public int getRankOrder() {
        return rankOrder;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public Suitability getSuitability() {
        return suitability;
    }

    public boolean isSelectedForRegistration() {
        return selectedForRegistration;
    }

    public void setSelectedForRegistration(boolean selectedForRegistration) {
        this.selectedForRegistration = selectedForRegistration;
    }
}
