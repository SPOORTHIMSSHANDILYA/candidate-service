package com.recruit.candidate_service.exceptions;


public class CandidateNotFoundException extends RuntimeException {

    public CandidateNotFoundException(Long id) {
        super("Candidate not found: " + id);   // id only, safe to log
    }
}