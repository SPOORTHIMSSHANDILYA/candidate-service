package com.recruit.candidate_service.controllers;
import com.recruit.candidate_service.models.CandidateRequest;
import com.recruit.candidate_service.models.CandidateResponse;
import com.recruit.candidate_service.models.CandidateSearchCriteria;
import com.recruit.candidate_service.models.CandidateStatusRequest;
import com.recruit.candidate_service.models.CandidateStats;
import com.recruit.candidate_service.models.CandidateSummary;
import com.recruit.candidate_service.models.PageResponse;
import com.recruit.candidate_service.services.CandidateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@RequestMapping("/api/v1/candidates")
public class CandidateController {

    private final CandidateService service;

    public CandidateController(CandidateService service) {
        this.service = service;
    }

    /**
     * Search + filter + page + sort. Archived candidates are excluded unless status is specified.
     * GET /api/v1/candidates?q=priya&skills=java,aws&skillMatch=ANY&status=ACTIVE,INACTIVE
     *                       &maxNoticeDays=30&city=Pune&minExperience=3&maxExperience=7
     *                       &openToWork=true&page=0&size=20&sort=createdAt,desc
     * -> 200, or 400 for invalid parameters
     */
    @GetMapping
    public ResponseEntity<PageResponse<CandidateSummary>> searchCandidates(
            @Valid @ModelAttribute CandidateSearchCriteria criteria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(service.search(criteria, page, size, sort));
    }

    //get candidate by id
    @GetMapping("/{id}")
    public ResponseEntity<CandidateResponse> getCandidateById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getCandidateById(id));
    }

    //create new candidate
    @PostMapping
    public ResponseEntity<CandidateResponse> createCandidates(@Valid @RequestBody CandidateRequest request) {
        CandidateResponse response = service.createCandidate(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    //update a candidate
    @PutMapping("/{id}")
    public ResponseEntity<CandidateResponse> updateCandidate(@PathVariable Long id,
                                                             @Valid @RequestBody CandidateRequest request) {
        return ResponseEntity.ok(service.updateCandidate(id, request));
    }

    // DELETE candidate
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable Long id) {
        service.deleteCandidate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CandidateResponse> updateCandidateStatus(@PathVariable Long id,
                                                                    @Valid @RequestBody CandidateStatusRequest request) {
        return ResponseEntity.ok(service.changeStatus(id, request));
    }

    @GetMapping("/stats")
    public ResponseEntity<CandidateStats> getStats() {
        return ResponseEntity.ok(service.getStats());
    }
}
