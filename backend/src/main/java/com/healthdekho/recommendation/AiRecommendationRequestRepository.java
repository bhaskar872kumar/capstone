package com.healthdekho.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecommendationRequestRepository extends JpaRepository<AiRecommendationRequest, Long> {

    long countByCustomer_Id(Long customerId);
}
