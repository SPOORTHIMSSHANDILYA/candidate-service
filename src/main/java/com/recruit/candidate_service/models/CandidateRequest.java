package com.recruit.candidate_service.models;

import jakarta.validation.constraints.*;
import java.util.List;

public record CandidateRequest(

        @NotBlank @Size(max = 100)
        String firstName,

        @NotBlank @Size(max = 100)
        String lastName,

        @NotBlank @Email @Size(max = 254)
        String email,

        @Size(max = 20)
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "must contain only digits, spaces, +, -, ( )")
        String phone,

        @Size(max = 150)
        String currentTitle,

        @Size(max = 150)
        String currentCompany,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String country,

        @Min(0) @Max(60)
        Integer yearsExperience,

        @Min(0) @Max(365)
        Integer noticePeriodDays,

        Boolean openToWork,

        @Size(max = 30)
        List<@NotBlank @Size(max = 50) String> skills,

        Long version
) {
}
