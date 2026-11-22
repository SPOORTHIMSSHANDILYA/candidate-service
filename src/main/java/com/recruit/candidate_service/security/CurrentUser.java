package com.recruit.candidate_service.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The recruiter who sent the current request, for the createdBy / updatedBy audit columns. */
public final class CurrentUser {

    /** Used when there is no signed-in user, e.g. a test or a background job. */
    public static final String SYSTEM = "system";

    private CurrentUser() {
    }

    public static String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return SYSTEM;
        }
        return auth.getName();
    }
}
