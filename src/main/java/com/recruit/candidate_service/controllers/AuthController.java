package com.recruit.candidate_service.controllers;

import com.recruit.candidate_service.models.CurrentUserResponse;
import com.recruit.candidate_service.models.LoginRequest;
import com.recruit.candidate_service.models.LoginResponse;
import com.recruit.candidate_service.security.JwtTokenService;
import com.recruit.candidate_service.security.RecruiterProperties;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokenService;
    private final RecruiterProperties recruiters;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenService tokenService,
                          RecruiterProperties recruiters) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.recruiters = recruiters;
    }

    /**
     * Checks username + password (BCrypt) once and returns a JWT.
     * 200 = token, 401 = wrong username or password (same message for both).
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));

        String username = auth.getName();
        JwtTokenService.IssuedToken token = tokenService.issue(username);
        log.info("Recruiter {} signed in; token valid until {}", username, token.expiresAt());
        return new LoginResponse(token.value(), "Bearer", token.expiresAt(),
                username, recruiters.displayNameOf(username));
    }

    /** Who owns the token. The frontend calls this after a page refresh to restore the session. */
    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        String username = authentication.getName();
        return new CurrentUserResponse(username, recruiters.displayNameOf(username));
    }
}
