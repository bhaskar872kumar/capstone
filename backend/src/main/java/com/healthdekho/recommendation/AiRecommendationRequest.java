package com.healthdekho.recommendation;

import com.healthdekho.customer.Customer;
import com.healthdekho.product.PolicyType;
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

import java.time.OffsetDateTime;

/** One recommendation interaction, with model metadata for auditing (spec 5.2 ai_recommendation_request). */
@Entity
@Table(name = "ai_recommendation_request")
public class AiRecommendationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "insurance_goals", nullable = false, length = 500)
    private String insuranceGoals;

    @Column(name = "preferred_term", length = 30)
    private String preferredTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_policy_type", length = 30)
    private PolicyType preferredPolicyType;

    @Column(name = "calculated_age", nullable = false)
    private Integer calculatedAge;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    @Column(name = "prompt_version", nullable = false, length = 30)
    private String promptVersion;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    protected AiRecommendationRequest() {
    }

    public AiRecommendationRequest(Customer customer, String insuranceGoals, String preferredTerm,
                                   PolicyType preferredPolicyType, int calculatedAge, String modelName,
                                   String modelVersion, String promptVersion, OffsetDateTime requestedAt) {
        this.customer = customer;
        this.insuranceGoals = insuranceGoals;
        this.preferredTerm = preferredTerm;
        this.preferredPolicyType = preferredPolicyType;
        this.calculatedAge = calculatedAge;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.promptVersion = promptVersion;
        this.requestedAt = requestedAt;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getInsuranceGoals() {
        return insuranceGoals;
    }

    public String getPreferredTerm() {
        return preferredTerm;
    }

    public PolicyType getPreferredPolicyType() {
        return preferredPolicyType;
    }

    public int getCalculatedAge() {
        return calculatedAge;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }
}
