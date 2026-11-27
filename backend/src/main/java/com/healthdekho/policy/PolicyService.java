package com.healthdekho.policy;

import com.healthdekho.policy.dto.PolicySummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PolicyService {

    private final CustomerPolicyRepository policyRepository;

    public PolicyService(CustomerPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    /** FR-1 B: only the authenticated customer's policies. Empty list when there are none. */
    @Transactional(readOnly = true)
    public List<PolicySummaryResponse> myPolicies(Long customerId) {
        return policyRepository.findAllForCustomer(customerId).stream()
                .map(PolicySummaryResponse::from)
                .toList();
    }
}
