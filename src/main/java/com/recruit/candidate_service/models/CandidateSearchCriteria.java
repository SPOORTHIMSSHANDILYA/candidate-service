package com.recruit.candidate_service.models;

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

         /** Candidate must have ALL of these skills. Comma-separated in the URL: skills=java,aws */
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

         Boolean openToWork

        ) { }