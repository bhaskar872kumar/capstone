package com.healthdekho.policy;

import com.healthdekho.policy.dto.PolicySummaryResponse;
import com.healthdekho.policy.dto.QuoteResponse;
import com.healthdekho.policy.dto.RegisterPolicyRequest;
import com.healthdekho.policy.dto.RegisterPolicyResponse;
import com.healthdekho.security.CurrentCustomer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PolicyController {

    private final PolicyService policyService;
    private final RegistrationService registrationService;
    private final CurrentCustomer currentCustomer;

    public PolicyController(PolicyService policyService, RegistrationService registrationService,
                            CurrentCustomer currentCustomer) {
        this.policyService = policyService;
        this.registrationService = registrationService;
        this.currentCustomer = currentCustomer;
    }

    /** FR-1 B: customer dashboard. */
    @GetMapping("/policies/mine")
    public List<PolicySummaryResponse> myPolicies() {
        return policyService.myPolicies(currentCustomer.id());
    }

    /** FR-3 A: pre-populated registration details for a product. */
    @GetMapping("/registrations/quote")
    public QuoteResponse quote(@RequestParam @NotBlank @Size(max = 30) String productCode) {
        return registrationService.quote(currentCustomer.id(), productCode);
    }

    /** FR-3 B: register the policy. */
    @PostMapping("/policies")
    public ResponseEntity<RegisterPolicyResponse> register(@Valid @RequestBody RegisterPolicyRequest request) {
        RegisterPolicyResponse response = registrationService.register(currentCustomer.id(), request);
        return ResponseEntity.created(URI.create("/api/policies/" + response.policyNumber())).body(response);
    }
}
