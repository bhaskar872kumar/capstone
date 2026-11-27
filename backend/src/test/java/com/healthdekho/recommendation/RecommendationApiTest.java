package com.healthdekho.recommendation;

import com.healthdekho.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-04 through the API, with the AI model replaced by a mock. */
@Transactional
class RecommendationApiTest extends ApiTestSupport {

    @MockitoBean
    private RecommendationAiClient aiClient;

    @Autowired
    private AiRecommendationRequestRepository requestRepository;

    @Test
    void AI_15_recommendationIsSavedAgainstCustomer() throws Exception {
        given(aiClient.suggest(any())).willReturn(Optional.of(new RecommendationAiResponse(List.of(
                new RecommendationAiResponse.Suggestion("HD-HEALTH-001", 1, "Annual family health cover.", "HIGH")))));
        long before = requestRepository.countByCustomer_Id(1001L);

        mockMvc.perform(post("/api/recommendations").session(login(AARAV)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("dateOfBirth", "1990-04-15",
                                "insuranceGoals", "Health cover for my family",
                                "preferredTermYears", 1,
                                "preferredPolicyType", "HEALTH"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculatedAge").value(36))
                .andExpect(jsonPath("$.source").value("AI"))
                .andExpect(jsonPath("$.items[0].productCode").value("HD-HEALTH-001"))
                .andExpect(jsonPath("$.items[0].recommendationReason").value("Annual family health cover."))
                .andExpect(jsonPath("$.items[0].recommendationItemId").isNumber());

        assertThat(requestRepository.countByCustomer_Id(1001L)).isEqualTo(before + 1);
    }

    @Test
    void AI_16_missingGoalsReturns400() throws Exception {
        mockMvc.perform(post("/api/recommendations").session(login(AARAV)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("dateOfBirth", "1990-04-15"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.insuranceGoals").exists());
    }

    @Test
    void AI_17_futureDateOfBirthReturns400() throws Exception {
        mockMvc.perform(post("/api/recommendations").session(login(AARAV)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("dateOfBirth", "2099-01-01", "insuranceGoals", "Health cover"))))
                .andExpect(status().isBadRequest());
    }
}
