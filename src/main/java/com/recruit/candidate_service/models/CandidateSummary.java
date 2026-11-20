package com.recruit.candidate_service.models;

import java.util.Set;
import java.util.UUID;

public record CandidateSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String currentTitle,
        String city,
        Integer yearsExperience,
        String country,
        Integer noticePeriodDays,
        boolean openToWork,
        Set<String> skills
) {
}
