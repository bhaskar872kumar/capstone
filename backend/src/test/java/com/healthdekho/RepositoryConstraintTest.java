package com.healthdekho;

import com.healthdekho.customer.CustomerRepository;
import com.healthdekho.policy.CustomerPolicy;
import com.healthdekho.policy.CustomerPolicyRepository;
import com.healthdekho.policy.PremiumInstallment;
import com.healthdekho.policy.PremiumInstallmentRepository;
import com.healthdekho.product.InsuranceProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Repository tests on PostgreSQL: the database itself enforces the spec's keys and checks. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RepositoryConstraintTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InsuranceProductRepository productRepository;

    @Autowired
    private CustomerPolicyRepository policyRepository;

    @Autowired
    private PremiumInstallmentRepository installmentRepository;

    @Test
    void REPO_01_samePolicyTwiceForCustomerIsBlocked() {
        CustomerPolicy duplicate = new CustomerPolicy("HDP-2026-9999",
                customerRepository.getReferenceById(1001L),
                productRepository.findByProductCode("HD-TERM-001").orElseThrow(),
                LocalDate.of(2026, 11, 25), LocalDate.of(2046, 11, 24), new BigDecimal("300000.00"));

        assertThatThrownBy(() -> policyRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void REPO_02_installmentNumberIsUniquePerPolicy() {
        CustomerPolicy policy = policyRepository.findById(3001L).orElseThrow();
        PremiumInstallment copy = new PremiumInstallment(policy, 1, LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 12, 20), new BigDecimal("1250.00"));

        assertThatThrownBy(() -> installmentRepository.saveAndFlush(copy))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void REPO_03_paidAmountCannotExceedScheduledAmount() {
        PremiumInstallment installment = installmentRepository.findById(4003L).orElseThrow();
        installment.setPaidAmount(new BigDecimal("5000.00"));

        assertThatThrownBy(() -> installmentRepository.saveAndFlush(installment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void REPO_04_sampleDataIsComplete() {
        assertThat(customerRepository.count()).isEqualTo(3);
        assertThat(installmentRepository.findByPolicy_IdOrderByInstallmentNumberAsc(3001L)).hasSize(240);
        assertThat(policyRepository.findProductIdsByCustomerId(1001L)).containsExactly(201L);
        assertThat(customerRepository.findByEmailIgnoreCase("AARAV.SHARMA@EXAMPLE.COM")).isPresent();
    }

    @Test
    void REPO_05_sequencesContinueAfterSampleNumbers() {
        assertThat(policyRepository.nextPolicyNumberValue()).isGreaterThanOrEqualTo(3L);
    }
}
