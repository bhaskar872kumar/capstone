package com.healthdekho.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiRecommendationItemRepository extends JpaRepository<AiRecommendationItem, Long> {

    /** A recommendation item, only if it was generated for this customer. */
    @Query("""
            select i from AiRecommendationItem i
            join fetch i.request r
            join fetch i.product
            where i.id = :itemId and r.customer.id = :customerId
            """)
    Optional<AiRecommendationItem> findOwned(@Param("itemId") Long itemId, @Param("customerId") Long customerId);

    List<AiRecommendationItem> findByRequest_IdOrderByRankOrderAsc(Long requestId);
}
