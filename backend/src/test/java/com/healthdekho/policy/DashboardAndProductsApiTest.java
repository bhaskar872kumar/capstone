package com.healthdekho.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.healthdekho.product.InsuranceProduct;
import com.healthdekho.product.InsuranceProductRepository;
import com.healthdekho.product.PolicyType;
import com.healthdekho.product.PremiumFrequency;
import com.healthdekho.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-02 dashboard and US-03 available policies. */
@Transactional
class DashboardAndProductsApiTest extends ApiTestSupport {

    @Autowired
    private InsuranceProductRepository productRepository;

    @Test
    void DASH_01_customerSeesOnlyOwnPolicies() throws Exception {
        mockMvc.perform(get("/api/policies/mine").session(login(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].policyNumber").value("HDP-2026-0001"))
                .andExpect(jsonPath("$[0].policyName").value("HealthDekho Secure Term"))
                .andExpect(jsonPath("$[0].policyTerm").value("20 years"))
                .andExpect(jsonPath("$[0].policyMaturityDate").value("2046-11-19"))
                .andExpect(jsonPath("$[*].policyNumber", not(hasItem("HDP-2026-0002"))));
    }

    @Test
    void DASH_02_customerWithoutPoliciesGetsEmptyList() throws Exception {
        mockMvc.perform(get("/api/policies/mine").session(login(ROHAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void DASH_03_dashboardNeedsLogin() throws Exception {
        mockMvc.perform(get("/api/policies/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void DISC_01_registeredProductIsExcluded() throws Exception {
        mockMvc.perform(get("/api/products/available").session(login(AARAV)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].productCode", hasItems("HD-HEALTH-001", "HD-SAV-001")))
                .andExpect(jsonPath("$[*].productCode", not(hasItem("HD-TERM-001"))));
    }

    @Test
    void DISC_02_inactiveProductIsExcluded() throws Exception {
        productRepository.save(new InsuranceProduct("HD-OLD-TEST", "Retired Test Plan", PolicyType.HEALTH, 12,
                PremiumFrequency.YEARLY, new BigDecimal("5000.00"), new BigDecimal("200000.00"), 18, 60, false));

        mockMvc.perform(get("/api/products/available").session(login(ROHAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].productCode", not(hasItem("HD-OLD-TEST"))));
    }

    @Test
    void DISC_03_availableListHasNoDuplicates() throws Exception {
        String body = mockMvc.perform(get("/api/products/available").session(login(ROHAN)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> codes = new ArrayList<>();
        for (JsonNode row : read(body)) {
            codes.add(row.get("productCode").asText());
        }
        assertThat(codes).hasSize(new HashSet<>(codes).size());
    }
}
