package com.recruit.candidate_service.models;

import com.recruit.candidate_service.constants.CandidateStatus;
import jakarta.validation.constraints.NotNull;

public record CandidateStatusRequest(
        @NotNull CandidateStatus status,
        @NotNull Long version
) {
}
