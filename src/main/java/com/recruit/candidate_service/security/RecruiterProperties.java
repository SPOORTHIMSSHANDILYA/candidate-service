package com.recruit.candidate_service.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Recruiters who may sign in, read from application.yml (app.security.recruiters).
 * Passwords come from environment variables, never from Git, and are BCrypt-hashed at startup.
 * A real product would keep users in a database or an identity provider (SSO) instead.
 */
@ConfigurationProperties(prefix = "app.security")
public record RecruiterProperties(List<Recruiter> recruiters) {

    public record Recruiter(String username, String displayName, String password) {
    }

    public RecruiterProperties {
        recruiters = recruiters == null ? List.of() : List.copyOf(recruiters);
    }

    /** Display name for the header, or the username if none is configured. */
    public String displayNameOf(String username) {
        return recruiters.stream()
                .filter(r -> r.username().equals(username))
                .map(Recruiter::displayName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(username);
    }
}
