package com.recruit.candidate_service.models;

import com.recruit.candidate_service.constants.CandidateStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CandidateResponse(
        Long id,
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
        Instant updatedAt,
        String candidateCode,
        CandidateStatus candidateStatus
) {
}
