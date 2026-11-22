package com.recruit.candidate_service.services;

import com.recruit.candidate_service.exceptions.BadRequestException;
import com.recruit.candidate_service.repositories.CandidateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private CandidateRepository repository;

    @InjectMocks
    private SkillService service;

    @Test
    void appliesDefaultAndExplicitLimitAndEscapesPrefixWildcards() {
        when(repository.findSkills("%", PageRequest.of(0, 20))).thenReturn(List.of("aws", "java"));
        assertEquals(List.of("aws", "java"), service.getCandidateSkills(null, null));
        verify(repository).findSkills("%", PageRequest.of(0, 20));

        when(repository.findSkills("ja%", PageRequest.of(0, 5))).thenReturn(List.of("java"));
        assertEquals(List.of("java"), service.getCandidateSkills(" JA ", 5));
        verify(repository).findSkills("ja%", PageRequest.of(0, 5));

        when(repository.findSkills("!%!_!!%", PageRequest.of(0, 50))).thenReturn(List.of());
        assertEquals(List.of(), service.getCandidateSkills("%_!", 50));
        verify(repository).findSkills("!%!_!!%", PageRequest.of(0, 50));
    }

    @Test
    void rejectsLimitsOutsideOneThroughFifty() {
        assertThrows(BadRequestException.class, () -> service.getCandidateSkills(null, 0));
        assertThrows(BadRequestException.class, () -> service.getCandidateSkills(null, 51));
    }
}
