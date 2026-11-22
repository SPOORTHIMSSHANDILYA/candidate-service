package com.recruit.candidate_service.models;

import java.time.Instant;

/** Sent back after a successful login. The client sends accessToken as "Authorization: Bearer ...". */
public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        String username,
        String displayName
) {
}
