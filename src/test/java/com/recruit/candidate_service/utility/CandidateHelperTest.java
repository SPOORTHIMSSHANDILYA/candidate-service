package com.recruit.candidate_service.utility;

import com.recruit.candidate_service.entities.Candidate;
import com.recruit.candidate_service.exceptions.BadRequestException;
import com.recruit.candidate_service.exceptions.CandidateNotFoundException;
import com.recruit.candidate_service.exceptions.DuplicateEmailException;
import com.recruit.candidate_service.repositories.CandidateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateHelperTest {

    @Mock
    private CandidateRepository repository;

    @InjectMocks
    private CandidateHelper helper;

    @Test
    void findOrThrowReturnsEntityOrThrowsNotFound() {
        Candidate candidate = new Candidate("Ananya", "Rao", "ananya@example.com");
        when(repository.findById(1L)).thenReturn(Optional.of(candidate));
        assertSame(candidate, helper.findOrThrow(1L));

        when(repository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(CandidateNotFoundException.class, () -> helper.findOrThrow(2L));
    }

    @Test
    void saveAndFlushReturnsSavedEntityAndMapsConstraintFailureToDuplicate() {
        Candidate candidate = new Candidate("Ananya", "Rao", "ananya@example.com");
        when(repository.saveAndFlush(candidate)).thenReturn(candidate);
        assertSame(candidate, helper.saveAndFlush(candidate));

        when(repository.saveAndFlush(candidate))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThrows(DuplicateEmailException.class, () -> helper.saveAndFlush(candidate));
    }

    @Test
    void normalizesEmailAndSkillsIncludingNullAndBlankValues() {
        assertEquals("user@example.com", CandidateHelper.normalizeEmail(" User@Example.com "));
        assertEquals(null, CandidateHelper.normalizeEmail(null));
        assertEquals(Set.of("java", "aws"), CandidateHelper.normalizeSkills(
                Arrays.asList("Java ", "AWS", null, "java", " ", "java")));
        assertEquals(Set.of(), CandidateHelper.normalizeSkills(null));
    }

    @Test
    void validatesSearchPageSizeAndExperienceBounds() {
        var criteria = new com.recruit.candidate_service.models.CandidateSearchCriteria(
                null, null, null, null, null, 3, 7, null, null, null, null);
        CandidateHelper.validateSearch(criteria, 0, 1);
        CandidateHelper.validateSearch(criteria, 0, 100);
        CandidateHelper.validateSearch(new com.recruit.candidate_service.models.CandidateSearchCriteria(
                null, null, null, null, null, 3, null, null, null, null, null), 0, 20);
        CandidateHelper.validateSearch(new com.recruit.candidate_service.models.CandidateSearchCriteria(
                null, null, null, null, null, null, 7, null, null, null, null), 0, 20);
        assertThrows(BadRequestException.class, () -> CandidateHelper.validateSearch(criteria, -1, 20));
        assertThrows(BadRequestException.class, () -> CandidateHelper.validateSearch(criteria, 0, 0));
        assertThrows(BadRequestException.class, () -> CandidateHelper.validateSearch(criteria, 0, 101));
        var invalidRange = new com.recruit.candidate_service.models.CandidateSearchCriteria(
                null, null, null, null, null, 8, 2, null, null, null, null);
        assertThrows(BadRequestException.class, () -> CandidateHelper.validateSearch(invalidRange, 0, 20));
    }

    @Test
    void parsesDefaultAndAllowedSortsWithStableIdTieBreaker() {
        Sort defaultSort = CandidateHelper.parseSort(null);
        assertEquals(Sort.Direction.DESC, defaultSort.getOrderFor("createdAt").getDirection());
        assertEquals(Sort.Direction.ASC, defaultSort.getOrderFor("id").getDirection());
        assertEquals(Sort.Direction.DESC, CandidateHelper.parseSort(" ").getOrderFor("createdAt").getDirection());

        Sort explicitSort = CandidateHelper.parseSort(" lastName , asc ");
        assertEquals(Sort.Direction.ASC, explicitSort.getOrderFor("lastName").getDirection());
        assertEquals(Sort.Direction.ASC, explicitSort.getOrderFor("id").getDirection());
        assertEquals(Sort.Direction.DESC, CandidateHelper.parseSort("firstName,desc")
                .getOrderFor("firstName").getDirection());
        assertEquals(Sort.Direction.ASC, CandidateHelper.parseSort("updatedAt")
                .getOrderFor("updatedAt").getDirection());
        assertEquals(Sort.Direction.DESC, CandidateHelper.parseSort("yearsExperience,desc")
                .getOrderFor("yearsExperience").getDirection());
        assertEquals(Sort.Direction.ASC, CandidateHelper.parseSort("createdAt,").getOrderFor("createdAt")
                .getDirection());
        assertThrows(BadRequestException.class, () -> CandidateHelper.parseSort("email"));
        assertThrows(BadRequestException.class, () -> CandidateHelper.parseSort("lastName,sideways"));
    }
}
