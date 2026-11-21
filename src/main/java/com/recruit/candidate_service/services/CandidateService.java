package com.recruit.candidate_service.services;

import com.recruit.candidate_service.constants.CandidateStatus;
import com.recruit.candidate_service.entities.Candidate;
import com.recruit.candidate_service.exceptions.BadRequestException;
import com.recruit.candidate_service.exceptions.CandidateNotFoundException;
import com.recruit.candidate_service.exceptions.DuplicateEmailException;
import com.recruit.candidate_service.exceptions.VersionConflictException;
import com.recruit.candidate_service.mapper.CandidateMapper;
import com.recruit.candidate_service.models.CandidateRequest;
import com.recruit.candidate_service.models.CandidateResponse;
import com.recruit.candidate_service.models.CandidateSearchCriteria;
import com.recruit.candidate_service.models.CandidateStatusRequest;
import com.recruit.candidate_service.models.CandidateStats;
import com.recruit.candidate_service.models.CandidateSummary;
import com.recruit.candidate_service.models.PageResponse;
import com.recruit.candidate_service.repositories.CandidateRepository;
import com.recruit.candidate_service.repositories.CandidateSpecifications;
import com.recruit.candidate_service.utility.CandidateHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;

import static com.recruit.candidate_service.utility.CandidateHelper.normalizeEmail;
import static com.recruit.candidate_service.utility.CandidateHelper.normalizeSkills;
import static com.recruit.candidate_service.utility.CandidateHelper.parseSort;
import static com.recruit.candidate_service.utility.CandidateHelper.validateSearch;

@Service
@Slf4j
public class CandidateService {

    private final CandidateRepository repository;

    private final CandidateMapper mapper;

    private final CandidateHelper helper;

    public CandidateService(CandidateRepository repository, CandidateMapper mapper, CandidateHelper helper){
        this.repository = repository;
        this.mapper = mapper;
        this.helper = helper;
    }


    @Transactional(readOnly = true)
    public PageResponse<CandidateSummary> search(CandidateSearchCriteria criteria,
                                                 int page, int size, String sort) {
        validateSearch(criteria, page, size);

        PageRequest pageable = PageRequest.of(page, size, parseSort(sort));
        Page<CandidateSummary> result = repository
                .findAll(CandidateSpecifications.matching(criteria), pageable)
                .map(mapper::toSummary);

        // counts only: search text can contain names (personal data)
        log.debug("Candidate search returned {} of {} results", result.getNumberOfElements(),
                result.getTotalElements());
        return PageResponse.from(result);
    }

    public CandidateResponse getCandidateById(Long id) {
        Candidate candidate = helper.findOrThrow(id);
        return mapper.toResponse(candidate);
    }

    @Transactional
    public CandidateResponse createCandidate(CandidateRequest request) {
        Candidate candidate = mapper.toEntity(request);
        String email = normalizeEmail(request.email());
        candidate.setEmail(email);

        if (repository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        candidate.replaceSkills(normalizeSkills(request.skills()));
        Candidate saved = helper.saveAndFlush(candidate);
        log.info("Created candidate id={}", saved.getId());
        return mapper.toResponse(saved);
    }

    @Transactional
    public CandidateResponse updateCandidate(Long id, CandidateRequest request) {
        Candidate candidate = helper.findOrThrow(id);

        // Optimistic locking: @Version alone only compares with the row we just loaded,
        // so a stale version from the client must be checked explicitly.
        if (request.version() == null) {
            throw new BadRequestException("version is required");
        }
        if (!request.version().equals(candidate.getVersion())) {
            throw new VersionConflictException();
        }

        String email = normalizeEmail(request.email());
        if (!email.equals(candidate.getEmail()) && repository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        mapper.updateEntity(request, candidate);
        candidate.setEmail(email);
        candidate.replaceSkills(normalizeSkills(request.skills()));

        Candidate saved = helper.saveAndFlush(candidate); // flush -> new version + updatedAt in response
        log.info("Updated candidate id={} version={}", saved.getId(), saved.getVersion());
        return mapper.toResponse(saved);
    }


    @Transactional
    public void deleteCandidate(Long id) {
        if (!repository.existsById(id)) {
            throw new CandidateNotFoundException(id);
        }
        repository.deleteById(id);                        // skills removed too
        log.info("Deleted candidate id={}", id);
    }

    @Transactional
    public CandidateResponse changeStatus(Long id, CandidateStatusRequest request) {
        Candidate candidate = helper.findOrThrow(id);

        if (request.version() == null) {
            throw new BadRequestException("version is required");
        }
        if (!request.version().equals(candidate.getVersion())) {
            throw new VersionConflictException();
        }

        candidate.setCandidateStatus(request.status());
        Candidate saved = helper.saveAndFlush(candidate);
        log.info("Updated candidate status id={} version={}", saved.getId(), saved.getVersion());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CandidateStats getStats() {
        Map<CandidateStatus, Long> countsByStatus = new EnumMap<>(CandidateStatus.class);
        for (CandidateStatus status : CandidateStatus.values()) {
            countsByStatus.put(status, 0L);
        }
        for (CandidateRepository.StatusCount count : repository.countCandidatesByStatus()) {
            countsByStatus.put(count.getStatus(), count.getCount());
        }

        Instant now = Instant.now();
        long addedInLast7Days = repository.countByCreatedAtBetween(
                now.minus(7, ChronoUnit.DAYS), now);
        return new CandidateStats(Map.copyOf(countsByStatus), addedInLast7Days);
    }
}
