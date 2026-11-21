package com.recruit.candidate_service.services;

import com.recruit.candidate_service.exceptions.BadRequestException;
import com.recruit.candidate_service.repositories.CandidateRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

import static com.recruit.candidate_service.constants.CandidateConstants.DEFAULT_LIMIT;
import static com.recruit.candidate_service.constants.CandidateConstants.MAX_LIMIT;

@Service
public class SkillService {

    private final CandidateRepository candidateRepository;

    public SkillService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    /** Distinct skills starting with prefix (case-insensitive), alphabetical, at most `limit`. */
    @Transactional(readOnly = true)
    public List<String> getCandidateSkills(String prefix, Integer limit) {
        int max = (limit == null) ? DEFAULT_LIMIT : limit;
        if (max < 1 || max > MAX_LIMIT) {
            throw new BadRequestException("limit must be between 1 and " + MAX_LIMIT);
        }

        String cleaned = (prefix == null) ? "" : prefix.trim().toLowerCase(Locale.ROOT);
        String pattern = escapeLike(cleaned) + "%";                 // "ja" -> "ja%"

        return candidateRepository.findSkills(pattern, PageRequest.of(0, max));
    }

    /** So "%" or "_" typed by the user is searched literally, not as a wildcard. */
    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
