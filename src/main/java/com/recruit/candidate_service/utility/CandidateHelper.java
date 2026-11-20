package com.recruit.candidate_service.utility;

import ch.qos.logback.classic.boolex.MarkerList;
import com.recruit.candidate_service.entities.Candidate;
import com.recruit.candidate_service.exceptions.BadRequestException;
import com.recruit.candidate_service.exceptions.CandidateNotFoundException;
import com.recruit.candidate_service.exceptions.DuplicateEmailException;
import com.recruit.candidate_service.models.CandidateSearchCriteria;
import com.recruit.candidate_service.repositories.CandidateRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.recruit.candidate_service.constants.CandidateConstants.*;


@Component
public class CandidateHelper {

    private final CandidateRepository repository;

    public CandidateHelper(CandidateRepository repository) {
        this.repository = repository;
    }

    private Candidate findOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new CandidateNotFoundException(id));
    }

    /**
     * saveAndFlush sends the SQL now (not at commit), so a duplicate email that slipped
     * past existsByEmail (two requests at the same time) is caught here as a 409.
     */
    public Candidate saveAndFlush(Candidate candidate) {
        try {
            return repository.saveAndFlush(candidate);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException();
        }
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Trim, lowercase, drop blanks and duplicates: "Java ", "java" -> "java". */
    public static Set<String> normalizeSkills(Collection<String> skills) {
        if (skills == null) {
            return Set.of();
        }
        return skills.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public static void validateSearch(CandidateSearchCriteria c, int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (c.minExperience() != null && c.maxExperience() != null
                && c.minExperience() > c.maxExperience()) {
            throw new BadRequestException("minExperience must not be greater than maxExperience");
        }
    }

    /**
     * "createdAt,desc" -> Sort. Only whitelisted fields.
     * id is always added last so pages are stable when values are equal.
     */
    public static Sort parseSort(String sort) {
        String[] parts = (sort == null || sort.isBlank() ? "createdAt,desc" : sort).split(",");
        String field = parts[0].trim();
        if (!SORTABLE_FIELDS.contains(field)) {
            throw new BadRequestException("sort must be one of " + SORTABLE_FIELDS);
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElseThrow(() -> new BadRequestException("sort direction must be asc or desc"));
        }
        return Sort.by(direction, field).and(Sort.by("id"));
    }

}
