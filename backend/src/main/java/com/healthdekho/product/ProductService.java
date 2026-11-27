package com.healthdekho.product;

import com.healthdekho.policy.CustomerPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final InsuranceProductRepository productRepository;
    private final CustomerPolicyRepository policyRepository;

    public ProductService(InsuranceProductRepository productRepository, CustomerPolicyRepository policyRepository) {
        this.productRepository = productRepository;
        this.policyRepository = policyRepository;
    }

    /**
     * FR-1 C: all active products minus the ones the customer already holds, without duplicates.
     * Also used by FR-2 so AI only ever sees products the customer can still buy.
     */
    @Transactional(readOnly = true)
    public List<InsuranceProduct> availableProducts(Long customerId) {
        Set<Long> heldProductIds = policyRepository.findProductIdsByCustomerId(customerId).stream()
                .collect(Collectors.toSet());

        return productRepository.findByActiveTrueOrderByPolicyNameAsc().stream()
                .filter(product -> !heldProductIds.contains(product.getId()))
                .collect(Collectors.toMap(
                        InsuranceProduct::getProductCode,
                        Function.identity(),
                        (first, duplicate) -> first,
                        LinkedHashMap::new))
                .values().stream()
                .toList();
    }
}
