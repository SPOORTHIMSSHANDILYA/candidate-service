package com.recruit.candidate_service.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT settings (app.jwt in application.yml).
 *
 * @param secret    HMAC key used to sign tokens; from the JWT_SECRET environment variable, at least 32 characters
 * @param expiresIn how long a token is valid, e.g. 8h; after that the recruiter signs in again
 * @param issuer    written into each token ("iss") and checked when a token comes back
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiresIn, String issuer) {

    public JwtProperties {
        expiresIn = expiresIn == null ? Duration.ofHours(8) : expiresIn;
        issuer = issuer == null || issuer.isBlank() ? "candidate-service" : issuer;
    }
}
