package com.healthdekho.policy;

import com.healthdekho.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REG_04: if saving the installment schedule fails, the policy row must not stay behind.
 * Not @Transactional on purpose, so the real commit/rollback is tested.
 */
class RegistrationRollbackTest extends ApiTestSupport {

    @MockitoBean
    private PremiumInstallmentRepository installmentRepository;

    @Autowired
    private CustomerPolicyRepository policyRepository;

    @Test
    void REG_04_failureWhileSavingScheduleRollsBackPolicy() throws Exception {
        given(installmentRepository.saveAll(anyList())).willThrow(new IllegalStateException("simulated database failure"));

        mockMvc.perform(post("/api/policies")
                        .session(login(ROHAN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productCode", "HD-HEALTH-001"))))
                .andExpect(status().isInternalServerError());

        // Rohan (1003) and HD-HEALTH-001 (202): nothing was committed.
        assertThat(policyRepository.existsByCustomer_IdAndProduct_Id(1003L, 202L)).isFalse();
    }
}
