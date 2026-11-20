package com.recruit.candidate_service.models;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CandidateResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String currentTitle,
        String currentCompany,
        String city,
        String country,
        Integer yearsExperience,
        Integer noticePeriodDays,
        boolean openToWork,
        Set<String> skills,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
