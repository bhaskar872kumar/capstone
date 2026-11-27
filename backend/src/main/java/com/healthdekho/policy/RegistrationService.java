package com.healthdekho.policy;

import com.healthdekho.common.PolicyNumbers;
import com.healthdekho.common.TermLabel;
import com.healthdekho.common.error.BusinessRuleException;
import com.healthdekho.common.error.DuplicateResourceException;
import com.healthdekho.common.error.NotFoundException;
import com.healthdekho.customer.Customer;
import com.healthdekho.customer.CustomerRepository;
import com.healthdekho.policy.dto.QuoteResponse;
import com.healthdekho.policy.dto.RegisterPolicyRequest;
import com.healthdekho.policy.dto.RegisterPolicyResponse;
import com.healthdekho.product.InsuranceProduct;
import com.healthdekho.product.InsuranceProductRepository;
import com.healthdekho.recommendation.AiRecommendationItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** FR-3: insurance policy registration. */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final CustomerRepository customerRepository;
    private final InsuranceProductRepository productRepository;
    private final CustomerPolicyRepository policyRepository;
    private final PremiumInstallmentRepository installmentRepository;
    private final AiRecommendationItemRepository recommendationItemRepository;
    private final Clock clock;

    public RegistrationService(CustomerRepository customerRepository,
                               InsuranceProductRepository productRepository,
                               CustomerPolicyRepository policyRepository,
                               PremiumInstallmentRepository installmentRepository,
                               AiRecommendationItemRepository recommendationItemRepository,
                               Clock clock) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.policyRepository = policyRepository;
        this.installmentRepository = installmentRepository;
        this.recommendationItemRepository = recommendationItemRepository;
        this.clock = clock;
    }

    /** FR-3 A: pre-populated registration details and the eligibility result, without saving anything. */
    @Transactional(readOnly = true)
    public QuoteResponse quote(Long customerId, String productCode) {
        Customer customer = loadCustomer(customerId);
        InsuranceProduct product = loadProduct(productCode);
        LocalDate startDate = LocalDate.now(clock);

        int installmentCount = installmentCountOf(product);
        LocalDate maturityDate = PolicyCalculator.maturityDate(startDate, product.getTermMonths());
        BigDecimal total = PolicyCalculator.totalPremium(product.getPremiumAmount(), installmentCount);
        int age = PolicyCalculator.ageOn(customer.getDateOfBirth(), startDate);
        Optional<String> problem = ineligibilityReason(customer, product, startDate, age);

        return new QuoteResponse(
                customer.getFullName(),
                customer.getDateOfBirth(),
                customer.getEmail(),
                customer.getPhone(),
                product.getProductCode(),
                product.getPolicyType(),
                product.getPolicyName(),
                product.getTermMonths(),
                TermLabel.of(product.getTermMonths()),
                startDate,
                maturityDate,
                product.getPremiumFrequency(),
                product.getPremiumAmount(),
                product.getSumInsured(),
                total,
                installmentCount,
                age,
                product.getMinEntryAge(),
                product.getMaxEntryAge(),
                problem.isEmpty(),
                problem.orElse(null));
    }

    /**
     * FR-3 B: validates, then saves the policy and its full installment schedule in ONE transaction.
     * Any failure rolls everything back.
     */
    @Transactional
    public RegisterPolicyResponse register(Long customerId, RegisterPolicyRequest request) {
        Customer customer = loadCustomer(customerId);
        InsuranceProduct product = loadProduct(request.productCode());
        LocalDate startDate = LocalDate.now(clock);

        if (!product.isActive()) {
            throw new BusinessRuleException("PRODUCT_INACTIVE", "This policy is no longer offered.");
        }
        if (customer.getDateOfBirth().isAfter(startDate)) {
            throw new BusinessRuleException("INVALID_DATE_OF_BIRTH", "Date of birth can't be in the future.");
        }
        int age = PolicyCalculator.ageOn(customer.getDateOfBirth(), startDate);
        if (!PolicyCalculator.isEligible(age, product.getMinEntryAge(), product.getMaxEntryAge())) {
            throw new BusinessRuleException("AGE_NOT_ELIGIBLE", ageMessage(product, age));
        }
        requirePositiveAmounts(product);
        int installmentCount = installmentCountOf(product);
        LocalDate maturityDate = PolicyCalculator.maturityDate(startDate, product.getTermMonths());
        if (!maturityDate.isAfter(startDate)) {
            throw new BusinessRuleException("INVALID_POLICY_DATES", "The maturity date must be after the start date.");
        }
        if (policyRepository.existsByCustomer_IdAndProduct_Id(customer.getId(), product.getId())) {
            throw duplicate(product);
        }

        String policyNumber = PolicyNumbers.format(startDate.getYear(), policyRepository.nextPolicyNumberValue());
        BigDecimal total = PolicyCalculator.totalPremium(product.getPremiumAmount(), installmentCount);
        CustomerPolicy policy = new CustomerPolicy(policyNumber, customer, product, startDate, maturityDate, total);
        try {
            policyRepository.saveAndFlush(policy);
        } catch (DataIntegrityViolationException ex) {
            // Two requests at the same moment: the unique key (customer_id, product_id) stops the second one.
            throw duplicate(product);
        }

        List<PremiumInstallment> installments = PolicyCalculator
                .schedule(startDate, product.getTermMonths(), product.getPremiumFrequency(), product.getPremiumAmount())
                .stream()
                .map(line -> new PremiumInstallment(policy, line.installmentNumber(), line.dueDate(),
                        line.gracePeriodEndDate(), line.scheduledAmount()))
                .toList();
        installmentRepository.saveAll(installments);

        if (request.recommendationItemId() != null) {
            recommendationItemRepository.findOwned(request.recommendationItemId(), customerId)
                    .filter(item -> item.getProduct().getId().equals(product.getId()))
                    .ifPresent(item -> item.setSelectedForRegistration(true));
        }

        log.info("Policy registered policyNumber={} customerId={} product={} installments={}",
                policyNumber, customerId, product.getProductCode(), installmentCount);

        return new RegisterPolicyResponse(
                policyNumber,
                "Your policy is registered. Your policy ID is " + policyNumber + ".",
                startDate,
                maturityDate,
                total,
                installmentCount,
                installments.getFirst().getDueDate());
    }

    private Optional<String> ineligibilityReason(Customer customer, InsuranceProduct product,
                                                 LocalDate startDate, int age) {
        if (!product.isActive()) {
            return Optional.of("This policy is no longer offered.");
        }
        if (customer.getDateOfBirth().isAfter(startDate)) {
            return Optional.of("Date of birth can't be in the future.");
        }
        if (!PolicyCalculator.isEligible(age, product.getMinEntryAge(), product.getMaxEntryAge())) {
            return Optional.of(ageMessage(product, age));
        }
        if (policyRepository.existsByCustomer_IdAndProduct_Id(customer.getId(), product.getId())) {
            return Optional.of("You have already registered for this policy.");
        }
        return Optional.empty();
    }

    private Customer loadCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer account not found."));
    }

    private InsuranceProduct loadProduct(String productCode) {
        String code = productCode == null ? "" : productCode.trim();
        return productRepository.findByProductCode(code)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "We couldn't find that policy."));
    }

    private static int installmentCountOf(InsuranceProduct product) {
        try {
            return PolicyCalculator.installmentCount(product.getTermMonths(), product.getPremiumFrequency());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("INVALID_PRODUCT_SETUP", ex.getMessage());
        }
    }

    private static void requirePositiveAmounts(InsuranceProduct product) {
        if (product.getPremiumAmount().signum() <= 0 || product.getSumInsured().signum() <= 0
                || product.getTermMonths() <= 0) {
            throw new BusinessRuleException("INVALID_PRODUCT_SETUP",
                    "This policy can't be registered because its premium, cover or term is not set up correctly.");
        }
    }

    private static String ageMessage(InsuranceProduct product, int age) {
        return "This policy is for ages %d to %d. Your age on the start date is %d."
                .formatted(product.getMinEntryAge(), product.getMaxEntryAge(), age);
    }

    private static DuplicateResourceException duplicate(InsuranceProduct product) {
        return new DuplicateResourceException("DUPLICATE_POLICY",
                "You have already registered for " + product.getPolicyName() + ".");
    }
}
