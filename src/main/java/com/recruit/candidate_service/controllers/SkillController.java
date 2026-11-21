package com.recruit.candidate_service.controllers;

import com.recruit.candidate_service.services.SkillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SkillController {
    private final SkillService service;

    public SkillController(SkillService service) {
        this.service = service;
    }

    @GetMapping("skills")
    public ResponseEntity<List<String>> getCandidateSkills(@RequestParam(required = false) String prefix,
                                                           @RequestParam (required = false) Integer limit) {
        return ResponseEntity.ok(service.getCandidateSkills(prefix, limit));
    }
}
