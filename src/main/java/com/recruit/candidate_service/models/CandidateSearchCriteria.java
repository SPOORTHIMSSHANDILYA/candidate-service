package com.recruit.candidate_service.models;

import com.recruit.candidate_service.constants.CandidateStatus;
import com.recruit.candidate_service.constants.SkillMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CandidateSearchCriteria
        (/** Free text: partial match on first name, last name, full name, email, title, company. */
         @Size(max = 100)
         String q,

         /** Exact email match. */
         @Email @Size(max = 254)
         String email,

         /** Comma-separated skills; match mode is controlled by skillMatch. */
         @Size(max = 10)
         List<@Size(max = 50) String> skills,

         @Size(max = 100)
         String city,

         @Size(max = 100)
         String country,

         @Min(0) @Max(60)
         Integer minExperience,

         @Min(0) @Max(60)
         Integer maxExperience,

         Boolean openToWork,

         /** Candidate may have any of these statuses. */
         List<CandidateStatus> status,

         @Min(0) @Max(365)
         Integer maxNoticeDays,

         /** ALL (default) requires every skill; ANY requires at least one. */
         SkillMatch skillMatch

        ) { }