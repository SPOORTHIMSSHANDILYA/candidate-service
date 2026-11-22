package com.recruit.candidate_service.models;

/** Who is signed in: GET /api/v1/auth/me */
public record CurrentUserResponse(String username, String displayName) {
}
