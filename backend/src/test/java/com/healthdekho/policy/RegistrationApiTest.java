package com.healthdekho.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdekho.product.InsuranceProduct;
import com.healthdekho.product.InsuranceProductRepository;
import com.healthdekho.product.PolicyType;
import com.healthdekho.product.PremiumFrequency;
import com.healthdekho.recommendation.AiRecommendationItemRepository;
import com.healthdekho.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-05 policy registration. Today is fixed at 2026-11-25. */
@Transactional
class RegistrationApiTest extends ApiTestSupport {

    @Autowired
    private CustomerPolicyRepository policyRepository;

    @Autowired
    private PremiumInstallmentRepository installmentRepository;

    @Autowired
    private InsuranceProductRepository productRepository;

    @Autowired
    private AiRecommendationItemRepository itemRepository;

    private org.springframework.test.web.servlet.ResultActions register(String email, Map<String, Object> body)
            throws Exception {
        return mockMvc.perform(post("/api/policies")
                .session(login(email))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    @Test
    void REG_01_registrationSavesPolicyAndFullSchedule() throws Exception {
        String body = register(AARAV, Map.of("productCode", "HD-SAV-001"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/policies/HDP-2026-\\d{4}")))
                .andExpect(jsonPath("$.policyNumber", matchesPattern("HDP-2026-\\d{4}")))
                .andExpect(jsonPath("$.policyStartDate").value("2026-11-25"))
                .andExpect(jsonPath("$.policyMaturityDate").value("2036-11-24"))
                .andExpect(jsonPath("$.installmentCount").value(40))
                .andExpect(jsonPath("$.totalPremiumPayable").value(300000.0))
                .andReturn().getResponse().getContentAsString();

        String policyNumber = read(body).get("policyNumber").asText();
        CustomerPolicy policy = policyRepository.findOwned(1001L, policyNumber).orElseThrow();
        List<PremiumInstallment> schedule = installmentRepository.findByPolicy_IdOrderByInstallmentNumberAsc(policy.getId());

        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(schedule).hasSize(40);
        assertThat(schedule.getFirst().getDueDate()).isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(schedule.getFirst().getGracePeriodEndDate()).isEqualTo(LocalDate.of(2026, 12, 25));
        assertThat(schedule.getLast().getDueDate()).isEqualTo(LocalDate.of(2036, 8, 25));
        assertThat(schedule).allSatisfy(installment -> {
            assertThat(installment.getScheduledAmount()).isEqualByComparingTo("7500.00");
            assertThat(installment.getPaidAmount()).isEqualByComparingTo("0.00");
            assertThat(installment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        });
    }

    @Test
    void REG_02_duplicateRegistrationReturns409() throws Exception {
        long before = policyRepository.count();
        register(AARAV, Map.of("productCode", "HD-TERM-001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_POLICY"));
        assertThat(policyRepository.count()).isEqualTo(before);
    }

    @Test
    void REG_03_ageOutsideEntryRangeReturns422() throws Exception {
        productRepository.save(new InsuranceProduct("HD-YOUTH-TEST", "Young Starter Test", PolicyType.HEALTH, 12,
                PremiumFrequency.YEARLY, new BigDecimal("6000.00"), new BigDecimal("300000.00"), 18, 25, true));

        register(AARAV, Map.of("productCode", "HD-YOUTH-TEST"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("AGE_NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.detail").value("This policy is for ages 18 to 25. Your age on the start date is 36."));
    }

    @Test
    void REG_06_unknownProductReturns404() throws Exception {
        register(AARAV, Map.of("productCode", "HD-NOPE-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void REG_07_registeringFromRecommendationMarksItemSelected() throws Exception {
        // Sample item 7002 recommended HD-SAV-001 to Aarav (request 6001).
        register(AARAV, Map.of("productCode", "HD-SAV-001", "recommendationItemId", 7002))
                .andExpect(status().isCreated());

        assertThat(itemRepository.findById(7002L).orElseThrow().isSelectedForRegistration()).isTrue();
    }

    @Test
    void REG_08_quotePrepopulatesFormFields() throws Exception {
        mockMvc.perform(get("/api/registrations/quote").param("productCode", "HD-SAV-001").session(login(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Aarav Sharma"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-04-15"))
                .andExpect(jsonPath("$.phone").value("9876501001"))
                .andExpect(jsonPath("$.policyStartDate").value("2026-11-25"))
                .andExpect(jsonPath("$.premiumFrequency").value("QUARTERLY"))
                .andExpect(jsonPath("$.installmentCount").value(40))
                .andExpect(jsonPath("$.eligible").value(true));
    }

    @Test
    void REG_09_quoteFlagsAlreadyRegisteredProduct() throws Exception {
        mockMvc.perform(get("/api/registrations/quote").param("productCode", "HD-TERM-001").session(login(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.ineligibleReason").value("You have already registered for this policy."));
    }

    @Test
    void REG_10_inactiveProductReturns422() throws Exception {
        productRepository.save(new InsuranceProduct("HD-OLD-TEST", "Retired Test Plan", PolicyType.HEALTH, 12,
                PremiumFrequency.YEARLY, new BigDecimal("5000.00"), new BigDecimal("200000.00"), 18, 60, false));

        register(ROHAN, Map.of("productCode", "HD-OLD-TEST"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PRODUCT_INACTIVE"));
    }

    @Test
    void REG_11_missingProductCodeReturns400() throws Exception {
        String body = mockMvc.perform(post("/api/policies")
                        .session(login(AARAV))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        JsonNode problem = read(body);
        assertThat(problem.get("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(problem.get("errors").has("productCode")).isTrue();
    }

    @Test
    void REG_12_registrationNeedsLogin() throws Exception {
        mockMvc.perform(post("/api/policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productCode", "HD-SAV-001"))))
                .andExpect(status().isUnauthorized());
    }
}
