package com.recruit.candidate_service.constants;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CandidateConstants {
    public static final int MAX_PAGE_SIZE = 100;

    public static final int DEFAULT_LIMIT = 20;

    public static final int MAX_LIMIT = 50;

    /** Only these fields can be used in ?sort= (anything else -> 400, never a raw DB error). */
    public static final Set<String> SORTABLE_FIELDS =
            Set.of("createdAt", "updatedAt", "lastName", "firstName", "yearsExperience");
}
