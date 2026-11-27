package com.healthdekho;

import com.healthdekho.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FLOW_01: login, browse available policies, check the quote, register, see it on the dashboard, log out. */
@Transactional
class FlowApiTest extends ApiTestSupport {

    @Test
    void FLOW_01_loginBrowseRegisterDashboardLogout() throws Exception {
        MockHttpSession session = login(ROHAN);

        mockMvc.perform(get("/api/policies/mine").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/products/available").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].productCode", hasItem("HD-SAV-001")));

        mockMvc.perform(get("/api/registrations/quote").param("productCode", "HD-SAV-001").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));

        String registered = mockMvc.perform(post("/api/policies").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productCode", "HD-SAV-001"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String policyNumber = read(registered).get("policyNumber").asText();

        mockMvc.perform(get("/api/policies/mine").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyNumber").value(policyNumber))
                .andExpect(jsonPath("$[0].policyStatus").value("ACTIVE"));

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
    }
}
