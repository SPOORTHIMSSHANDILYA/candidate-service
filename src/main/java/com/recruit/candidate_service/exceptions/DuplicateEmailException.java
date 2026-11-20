package com.recruit.candidate_service.exceptions;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("Candidate with this email already exists");
    }
}
