package com.recruit.candidate_service.controllers;
import com.recruit.candidate_service.models.*;
import com.recruit.candidate_service.services.CandidateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
public class CandidateController {

    private final CandidateService service;

    public CandidateController(CandidateService service) {
        this.service = service;
    }

    /**
     * Search + filter + page + sort. All parameters optional; no parameters = all candidates, paged.
     * GET /api/v1/candidates?q=priya&skills=java,aws&city=Pune&minExperience=3&maxExperience=7
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
    public ResponseEntity<CandidateSummary> getCandidateById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getCandidateById(id));
    }

    //create new candidate
    @PostMapping
    public ResponseEntity<CandidateResponse> createCandidates(@Valid @RequestBody CandidateRequest request) {
        CandidateResponse response = service.createCandidate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
}

