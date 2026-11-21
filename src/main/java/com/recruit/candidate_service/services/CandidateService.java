package com.recruit.candidate_service.services;

import com.recruit.candidate_service.entities.Candidate;
import com.recruit.candidate_service.exceptions.CandidateNotFoundException;
import com.recruit.candidate_service.exceptions.DuplicateEmailException;
import com.recruit.candidate_service.exceptions.VersionConflictException;
import com.recruit.candidate_service.mapper.CandidateMapper;
import com.recruit.candidate_service.models.*;
import com.recruit.candidate_service.repositories.CandidateRepository;
import com.recruit.candidate_service.repositories.CandidateSpecifications;
import com.recruit.candidate_service.utility.CandidateHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.recruit.candidate_service.utility.CandidateHelper.*;

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

    public CandidateSummary getCandidateById(Long id) {
        Candidate canddidate = repository.findById(id).orElseThrow(() -> new RuntimeException("Candidate not found"));
        return mapper.toSummary(canddidate);
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
        return mapper.toResponse(saved);
    }

    @Transactional
    public CandidateResponse updateCandidate(Long id, CandidateRequest request) {
        Candidate candidate = findOrThrow(id);

        // Optimistic locking: @Version alone only compares with the row we just loaded,
        // so a stale version from the client must be checked explicitly.
        if (request.version() == null || !request.version().equals(candidate.getVersion())) {
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
        repository.deleteById(id);                        // skills + applications removed too
        log.info("Deleted candidate id={}", id);
    }


    private Candidate findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new CandidateNotFoundException(id));
    }
}
