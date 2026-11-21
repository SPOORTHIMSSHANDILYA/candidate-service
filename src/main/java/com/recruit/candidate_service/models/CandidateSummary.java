package com.recruit.candidate_service.models;

import com.recruit.candidate_service.constants.CandidateStatus;

import java.util.Set;

public record CandidateSummary(
        Long id,
        String firstName,
        String lastName,
        String email,
        String currentTitle,
        String city,
        Integer yearsExperience,
        String country,
        Integer noticePeriodDays,
        boolean openToWork,
        Set<String> skills,
        String candidateCode,
        CandidateStatus candidateStatus
) {
}