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
import com.recruit.candidate_service.models.CandidateSummary;
import com.recruit.candidate_service.repositories.CandidateRepository;
import com.recruit.candidate_service.security.CurrentUser;
import com.recruit.candidate_service.utility.CandidateHelper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private CandidateRepository repository;

    @Mock
    private CandidateMapper mapper;

    @Mock
    private CandidateHelper helper;

    @InjectMocks
    private CandidateService service;

    @BeforeEach
    void clearSecurityContextBeforeTest() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContextAfterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchMapsPageAndReturnsPagingMetadata() {
        Candidate entity = candidate(1L, "ananya@example.com", 0L);
        CandidateSummary summary = summary();
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<Candidate>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));
        when(mapper.toSummary(entity)).thenReturn(summary);

        var result = service.search(criteria(), 0, 20, "createdAt,desc");

        assertEquals(List.of(summary), result.content());
        assertEquals(0, result.page());
        assertEquals(20, result.size());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(false, result.hasNext());
    }

    @Test
    void searchRejectsInvalidPageRangeAndSortWithoutCallingRepository() {
        assertThrows(BadRequestException.class, () -> service.search(criteria(), -1, 20, "createdAt"));
        assertThrows(BadRequestException.class, () -> service.search(criteria(), 0, 101, "createdAt"));
        CandidateSearchCriteria reversedRange = new CandidateSearchCriteria(null, null, null, null,
                null, 8, 2, null, null, null, null);
        assertThrows(BadRequestException.class, () -> service.search(reversedRange, 0, 20, "createdAt"));
        assertThrows(BadRequestException.class, () -> service.search(criteria(), 0, 20, "email"));
        assertThrows(BadRequestException.class,
                () -> service.search(criteria(), 0, 20, "lastName,sideways"));
        verify(repository, never()).findAll(
                org.mockito.ArgumentMatchers.<Specification<Candidate>>any(), any(Pageable.class));
    }

    @Test
    void getByIdReturnsMappedCandidateAndPropagatesMissingId() {
        Candidate found = candidate(1L, "ananya@example.com", 0L);
        CandidateResponse response = response();
        when(helper.findOrThrow(1L)).thenReturn(found);
        when(mapper.toResponse(found)).thenReturn(response);
        assertEquals(response, service.getCandidateById(1L));
        when(helper.findOrThrow(2L)).thenThrow(new CandidateNotFoundException(2L));
        assertThrows(CandidateNotFoundException.class, () -> service.getCandidateById(2L));
    }

    @Test
    void createNormalizesEmailSkillsAndSetsAuditFields() {
        CandidateRequest request = request(" ANANYA@EXAMPLE.COM ", null);
        Candidate entity = candidate(1L, null, 0L);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.existsByEmail("ananya@example.com")).thenReturn(false);
        when(helper.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response());

        assertEquals(response(), service.createCandidate(request));

        assertEquals("ananya@example.com", entity.getEmail());
        assertEquals(Set.of("java", "aws"), entity.getSkills());
        assertEquals(CurrentUser.SYSTEM, entity.getCreatedBy());
        assertEquals(CurrentUser.SYSTEM, entity.getUpdatedBy());
        verify(helper).saveAndFlush(entity);
    }

    @Test
    void createRejectsDuplicateEmail() {
        CandidateRequest request = request("ANANYA@example.com", null);
        when(mapper.toEntity(request)).thenReturn(candidate(1L, null, 0L));
        when(repository.existsByEmail("ananya@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.createCandidate(request));

        verify(helper, never()).saveAndFlush(any(Candidate.class));
    }

    @Test
    void updateRequiresCurrentVersionAndUniqueEmail() {
        Candidate candidate = candidate(1L, "ananya@example.com", 2L);
        when(helper.findOrThrow(1L)).thenReturn(candidate);
        assertThrows(BadRequestException.class,
                () -> service.updateCandidate(1L, request("ananya@example.com", null)));
        assertThrows(VersionConflictException.class,
                () -> service.updateCandidate(1L, request("ananya@example.com", 1L)));
        CandidateRequest takenEmail = request("other@example.com", 2L);
        when(repository.existsByEmail("other@example.com")).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> service.updateCandidate(1L, takenEmail));
        verify(mapper, never()).updateEntity(any(CandidateRequest.class), any(Candidate.class));
    }

    @Test
    void updateSavesMutableFieldsAndReplacesSkills() {
        Candidate candidate = candidate(1L, "ananya@example.com", 2L);
        candidate.replaceSkills(Set.of("old"));
        CandidateRequest request = request(" updated@example.com ", 2L);
        when(helper.findOrThrow(1L)).thenReturn(candidate);
        when(repository.existsByEmail(anyString())).thenReturn(false);
        when(helper.saveAndFlush(candidate)).thenReturn(candidate);
        when(mapper.toResponse(candidate)).thenReturn(response());

        assertEquals(response(), service.updateCandidate(1L, request));

        assertEquals("updated@example.com", candidate.getEmail());
        assertEquals(Set.of("java", "aws"), candidate.getSkills());
        assertEquals(CurrentUser.SYSTEM, candidate.getUpdatedBy());
        verify(mapper).updateEntity(request, candidate);
        verify(helper).saveAndFlush(candidate);
    }

    @Test
    void updateAllowsExistingEmailWhenItIsTheCandidatesOwn() {
        Candidate candidate = candidate(1L, "ananya@example.com", 2L);
        CandidateRequest request = request("ANANYA@example.com", 2L);
        when(helper.findOrThrow(1L)).thenReturn(candidate);
        when(helper.saveAndFlush(candidate)).thenReturn(candidate);
        when(mapper.toResponse(candidate)).thenReturn(response());

        service.updateCandidate(1L, request);

        verify(repository, never()).existsByEmail(anyString());
    }

    @Test
    void deleteRequiresExistingCandidate() {
        when(repository.existsById(99L)).thenReturn(false);
        assertThrows(CandidateNotFoundException.class, () -> service.deleteCandidate(99L));
        verify(repository, never()).deleteById(99L);

        when(repository.existsById(1L)).thenReturn(true);
        service.deleteCandidate(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void changeStatusChecksVersionAndPersistsStatus() {
        Candidate candidate = candidate(1L, "ananya@example.com", 3L);
        when(helper.findOrThrow(1L)).thenReturn(candidate);
        assertThrows(BadRequestException.class, () -> service.changeStatus(1L,
                new CandidateStatusRequest(CandidateStatus.INACTIVE, null)));
        assertThrows(VersionConflictException.class, () -> service.changeStatus(1L,
                new CandidateStatusRequest(CandidateStatus.INACTIVE, 2L)));
        when(helper.saveAndFlush(candidate)).thenReturn(candidate);
        when(mapper.toResponse(candidate)).thenReturn(response());

        service.changeStatus(1L, new CandidateStatusRequest(CandidateStatus.INACTIVE, 3L));

        assertEquals(CandidateStatus.INACTIVE, candidate.getCandidateStatus());
        assertEquals(CurrentUser.SYSTEM, candidate.getUpdatedBy());
        verify(helper).saveAndFlush(candidate);
    }

    @Test
    void statsIncludesAllStatusesEvenWhenRepositoryOmitsZeroCounts() {
        when(repository.countCandidatesByStatus()).thenReturn(List.of(
                statusCount(CandidateStatus.ACTIVE, 2),
                statusCount(CandidateStatus.ARCHIVED, 1)));
        when(repository.countByCreatedAtBetween(any(Instant.class), any(Instant.class))).thenReturn(2L);
        when(repository.countByOpenToWorkTrue()).thenReturn(1L);

        var stats = service.getStats();

        assertEquals(3L, stats.total());
        assertEquals(2L, stats.countsByStatus().get(CandidateStatus.ACTIVE));
        assertEquals(0L, stats.countsByStatus().get(CandidateStatus.INACTIVE));
        assertEquals(1L, stats.countsByStatus().get(CandidateStatus.ARCHIVED));
        assertEquals(2L, stats.addedInLast7Days());
        assertEquals(1L, stats.openToWork());
    }

    private static CandidateRepository.StatusCount statusCount(CandidateStatus status, long count) {
        return new CandidateRepository.StatusCount() {
            @Override
            public CandidateStatus getStatus() {
                return status;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    private static Candidate candidate(Long id, String email, Long version) {
        Candidate candidate = new Candidate("Ananya", "Rao", "ananya@example.com");
        ReflectionTestUtils.setField(candidate, "id", id);
        ReflectionTestUtils.setField(candidate, "email", email);
        ReflectionTestUtils.setField(candidate, "version", version);
        return candidate;
    }

    private static CandidateRequest request(String email, Long version) {
        return new CandidateRequest("Ananya", "Rao", email, null, "Engineer", "Example Co",
                "Bengaluru", "India", 5, 30, true, List.of(" Java ", "AWS", "java"), version);
    }

    private static CandidateSearchCriteria criteria() {
        return new CandidateSearchCriteria(null, null, null, null, null,
                null, null, null, null, null, null);
    }

    private static CandidateSummary summary() {
        return new CandidateSummary(1L, "Ananya", "Rao", "ananya@example.com",
                "Engineer", "Bengaluru", 5, "India", 30, true,
                Set.of("java"), "CAN-00001", CandidateStatus.ACTIVE);
    }

    private static CandidateResponse response() {
        return new CandidateResponse(1L, "Ananya", "Rao", "ananya@example.com",
                null, "Engineer", "Example Co", "Bengaluru", "India", 5, 30,
                true, Set.of("java", "aws"), 3L, Instant.EPOCH, Instant.EPOCH,
                "CAN-00001", CandidateStatus.ACTIVE);
    }
}
