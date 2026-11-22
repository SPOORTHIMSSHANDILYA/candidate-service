package com.recruit.candidate_service.controllers;

import com.recruit.candidate_service.constants.CandidateStatus;
import com.recruit.candidate_service.models.CandidateRequest;
import com.recruit.candidate_service.models.CandidateResponse;
import com.recruit.candidate_service.models.CandidateSearchCriteria;
import com.recruit.candidate_service.models.CandidateStats;
import com.recruit.candidate_service.models.CandidateStatusRequest;
import com.recruit.candidate_service.models.CandidateSummary;
import com.recruit.candidate_service.models.PageResponse;
import com.recruit.candidate_service.services.CandidateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateControllerTest {

    @Mock
    private CandidateService service;

    @InjectMocks
    private CandidateController controller;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void searchPassesAllArgumentsThroughToService() {
        CandidateSearchCriteria criteria = criteria();
        PageResponse<CandidateSummary> page =
                new PageResponse<>(List.of(summary()), 1, 10, 11, 2, false);
        when(service.search(criteria, 1, 10, "lastName,asc")).thenReturn(page);

        var response = controller.searchCandidates(criteria, 1, 10, "lastName,asc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(page, response.getBody());
        verify(service).search(criteria, 1, 10, "lastName,asc");
    }

    @Test
    void getByIdReturnsServiceResponse() {
        CandidateResponse candidate = candidate(7L);
        when(service.getCandidateById(7L)).thenReturn(candidate);

        var response = controller.getCandidateById(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(candidate, response.getBody());
        verify(service).getCandidateById(7L);
    }

    @Test
    void createReturnsCreatedLocationAndCandidate() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setRequestURI("/api/v1/candidates");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        CandidateRequest body = request();
        CandidateResponse created = candidate(7L);
        when(service.createCandidate(body)).thenReturn(created);

        var response = controller.createCandidates(body);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("http://localhost:8080/api/v1/candidates/7",
                response.getHeaders().getLocation().toString());
        assertSame(created, response.getBody());
        verify(service).createCandidate(body);
    }

    @Test
    void updateReturnsServiceResponse() {
        CandidateRequest body = request();
        CandidateResponse updated = candidate(7L);
        when(service.updateCandidate(7L, body)).thenReturn(updated);

        var response = controller.updateCandidate(7L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(updated, response.getBody());
        verify(service).updateCandidate(7L, body);
    }

    @Test
    void deleteReturnsNoContent() {
        var response = controller.deleteCandidate(7L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service).deleteCandidate(7L);
    }

    @Test
    void changeStatusReturnsServiceResponse() {
        CandidateStatusRequest body = new CandidateStatusRequest(CandidateStatus.INACTIVE, 0L);
        CandidateResponse updated = candidate(7L);
        when(service.changeStatus(7L, body)).thenReturn(updated);

        var response = controller.updateCandidateStatus(7L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(updated, response.getBody());
        verify(service).changeStatus(7L, body);
    }

    @Test
    void statsReturnsServiceStats() {
        CandidateStats stats = new CandidateStats(1,
                Map.of(CandidateStatus.ACTIVE, 1L), 1, 1);
        when(service.getStats()).thenReturn(stats);

        var response = controller.getStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(stats, response.getBody());
        verify(service).getStats();
    }

    private static CandidateSearchCriteria criteria() {
        return new CandidateSearchCriteria(null, null, null, null, null,
                null, null, null, null, null, null);
    }

    private static CandidateRequest request() {
        return new CandidateRequest("Ananya", "Rao", "ananya@example.com", null,
                null, null, null, null, 5, 30, true, List.of("java"), 0L);
    }

    private static CandidateResponse candidate(Long id) {
        return new CandidateResponse(id, "Ananya", "Rao", "ananya@example.com",
                null, null, null, null, null, 5, 30, true, Set.of("java"),
                0L, Instant.EPOCH, Instant.EPOCH, "CAN-00007", CandidateStatus.ACTIVE);
    }

    private static CandidateSummary summary() {
        return new CandidateSummary(7L, "Ananya", "Rao", "ananya@example.com",
                "Engineer", "Bengaluru", 5, "India", 30, true,
                Set.of("java"), "CAN-00007", CandidateStatus.ACTIVE);
    }
}
