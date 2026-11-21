package com.recruit.candidate_service.models;

import com.recruit.candidate_service.constants.CandidateStatus;

import java.util.Map;

public record CandidateStats(
        Map<CandidateStatus, Long> countsByStatus,
        long addedInLast7Days
) {
}
