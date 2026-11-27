package com.healthdekho.recommendation.dto;

import com.healthdekho.product.PolicyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** FR-2 D inputs: date of birth, goals and preferences, preferred term, insurance type. */
public record RecommendationRequest(
        @NotNull(message = "Enter your date of birth.")
        @Past(message = "Date of birth must be in the past.")
        LocalDate dateOfBirth,

        @NotBlank(message = "Tell us your insurance goals.")
        @Size(min = 5, max = 500, message = "Goals must be between 5 and 500 characters.")
        String insuranceGoals,

        @Min(value = 1, message = "Preferred term must be at least 1 year.")
        @Max(value = 40, message = "Preferred term can be at most 40 years.")
        Integer preferredTermYears,

        PolicyType preferredPolicyType) {
}
