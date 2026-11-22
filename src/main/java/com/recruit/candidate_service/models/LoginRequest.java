package com.recruit.candidate_service.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/v1/auth/login */
public record LoginRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 200) String password
) {
    // Never print the password, e.g. if this object is ever logged.
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
