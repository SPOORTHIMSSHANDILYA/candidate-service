package com.recruit.candidate_service.exceptions;

public class VersionConflictException extends RuntimeException {

    public VersionConflictException() {
        super("Candidate was modified by another user, reload and try again");
    }
}
