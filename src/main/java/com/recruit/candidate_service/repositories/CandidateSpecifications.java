package com.recruit.candidate_service.repositories;

import com.recruit.candidate_service.entities.Candidate;
import com.recruit.candidate_service.models.CandidateSearchCriteria;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public final class CandidateSpecifications {

    private static final char LIKE_ESCAPE = '!';

    private CandidateSpecifications() {
        // utility class
    }

    /** Combines every filter that has a value. No filters -> matches all candidates. */
    public static Specification<Candidate> matching(CandidateSearchCriteria c) {
        List<Specification<Candidate>> specs = new ArrayList<>();

        if (hasText(c.q()))            specs.add(matchesText(c.q()));
        if (hasText(c.email()))        specs.add(hasEmail(c.email()));
        if (c.skills() != null)        c.skills().stream()
                .filter(CandidateSpecifications::hasText)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .forEach(skill -> specs.add(hasSkill(skill)));
        if (hasText(c.city()))         specs.add(hasCity(c.city()));
        if (hasText(c.country()))      specs.add(hasCountry(c.country()));
        if (c.minExperience() != null) specs.add(experienceAtLeast(c.minExperience()));
        if (c.maxExperience() != null) specs.add(experienceAtMost(c.maxExperience()));
        if (c.openToWork() != null)    specs.add(openToWork(c.openToWork()));

        return Specification.allOf(specs);
    }

    // ------------------------------------------------------------------ filters

    /** Partial match on first name, last name, "first last", email, current title or company. */
    static Specification<Candidate> matchesText(String q) {
        String pattern = "%" + escapeLike(q.trim()) + "%";
        return (root, query, cb) -> {
            Expression<String> fullName =
                    cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"));
            return cb.or(
                    cb.like(root.get("firstName"), pattern, LIKE_ESCAPE),
                    cb.like(root.get("lastName"), pattern, LIKE_ESCAPE),
                    cb.like(fullName, pattern, LIKE_ESCAPE),
                    cb.like(root.get("email"), pattern, LIKE_ESCAPE),
                    cb.like(root.get("currentTitle"), pattern, LIKE_ESCAPE),
                    cb.like(root.get("currentCompany"), pattern, LIKE_ESCAPE)
            );
        };
    }

    static Specification<Candidate> hasEmail(String email) {
        String value = email.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(root.get("email"), value);
    }

    /** Candidate's skills collection contains this skill (skills are stored lowercase). */
    static Specification<Candidate> hasSkill(String skill) {
        return (root, query, cb) -> cb.isMember(skill, root.<Set<String>>get("skills"));
    }

    static Specification<Candidate> hasCity(String city) {
        String value = city.trim();
        return (root, query, cb) -> cb.equal(root.get("city"), value);
    }

    static Specification<Candidate> hasCountry(String country) {
        String value = country.trim();
        return (root, query, cb) -> cb.equal(root.get("country"), value);
    }

    static Specification<Candidate> experienceAtLeast(int years) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("yearsExperience"), years);
    }

    static Specification<Candidate> experienceAtMost(int years) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("yearsExperience"), years);
    }

    static Specification<Candidate> openToWork(boolean value) {
        return (root, query, cb) -> cb.equal(root.get("openToWork"), value);
    }

    // ------------------------------------------------------------------ helpers

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /** So that a user typing "%" or "_" searches for that character, not a wildcard. */
    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}

