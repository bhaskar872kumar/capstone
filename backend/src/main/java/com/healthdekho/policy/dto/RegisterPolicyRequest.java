package com.healthdekho.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * FR-3: register a policy for the logged-in customer. Start date is always today (FR-3 A);
 * every other value comes from the product, never from the client.
 */
public record RegisterPolicyRequest(
        @NotBlank(message = "Choose a policy to register.")
        @Size(max = 30)
        String productCode,

        @Positive
        Long recommendationItemId) {
}
