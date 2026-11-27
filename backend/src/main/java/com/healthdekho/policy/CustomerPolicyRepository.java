package com.healthdekho.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerPolicyRepository extends JpaRepository<CustomerPolicy, Long> {

    /** FR-1 B: the customer's policies with their product, newest first. */
    @Query("""
            select p from CustomerPolicy p
            join fetch p.product
            where p.customer.id = :customerId
            order by p.startDate desc, p.id desc
            """)
    List<CustomerPolicy> findAllForCustomer(@Param("customerId") Long customerId);

    @Query("select p.product.id from CustomerPolicy p where p.customer.id = :customerId")
    List<Long> findProductIdsByCustomerId(@Param("customerId") Long customerId);

    boolean existsByCustomer_IdAndProduct_Id(Long customerId, Long productId);

    /** A policy by its identifier, only if it belongs to this customer. */
    @Query("""
            select p from CustomerPolicy p
            join fetch p.product
            join fetch p.customer c
            where p.policyNumber = :policyNumber and c.id = :customerId
            """)
    Optional<CustomerPolicy> findOwned(@Param("customerId") Long customerId,
                                       @Param("policyNumber") String policyNumber);

    @Query(value = "select nextval('policy_number_seq')", nativeQuery = true)
    Long nextPolicyNumberValue();
}
