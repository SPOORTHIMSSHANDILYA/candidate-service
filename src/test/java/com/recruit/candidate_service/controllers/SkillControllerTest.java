package com.recruit.candidate_service.controllers;

import com.recruit.candidate_service.services.SkillService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillControllerTest {

    @Mock
    private SkillService service;

    @InjectMocks
    private SkillController controller;

    @Test
    void forwardsOptionalPrefixAndLimitAndReturnsSkills() {
        List<String> skills = List.of("java", "javascript");
        when(service.getCandidateSkills("ja", 10)).thenReturn(skills);

        var response = controller.getCandidateSkills("ja", 10);

        assertEquals(200, response.getStatusCode().value());
        assertSame(skills, response.getBody());
        verify(service).getCandidateSkills("ja", 10);
    }

    @Test
    void forwardsMissingOptionalParameters() {
        when(service.getCandidateSkills(null, null)).thenReturn(List.of());

        var response = controller.getCandidateSkills(null, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of(), response.getBody());
        verify(service).getCandidateSkills(null, null);
    }
}
