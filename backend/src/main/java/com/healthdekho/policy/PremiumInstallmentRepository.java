package com.healthdekho.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PremiumInstallmentRepository extends JpaRepository<PremiumInstallment, Long> {

    List<PremiumInstallment> findByPolicy_IdOrderByInstallmentNumberAsc(Long policyId);
}
