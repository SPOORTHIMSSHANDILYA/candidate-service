package com.recruit.candidate_service.controllers;

import com.recruit.candidate_service.models.LoginRequest;
import com.recruit.candidate_service.security.JwtTokenService;
import com.recruit.candidate_service.security.RecruiterProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService tokenService;

    @Test
    void loginAuthenticatesIssuesBearerTokenAndResolvesDisplayName() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "recruiter", "ignored", List.of());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        Instant expires = Instant.parse("2030-01-01T00:00:00Z");
        when(tokenService.issue("recruiter"))
                .thenReturn(new JwtTokenService.IssuedToken("signed-token", expires));
        RecruiterProperties recruiters = new RecruiterProperties(List.of(
                new RecruiterProperties.Recruiter("recruiter", "Recruiter Name", "password")));
        AuthController controller = new AuthController(authenticationManager, tokenService, recruiters);

        var result = controller.login(new LoginRequest("recruiter", "password"));

        assertEquals("signed-token", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(expires, result.expiresAt());
        assertEquals("recruiter", result.username());
        assertEquals("Recruiter Name", result.displayName());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).issue("recruiter");
    }

    @Test
    void meReturnsConfiguredDisplayNameOrFallsBackToUsername() {
        RecruiterProperties recruiters = new RecruiterProperties(List.of(
                new RecruiterProperties.Recruiter("known", "Known User", "password"),
                new RecruiterProperties.Recruiter("blank", " ", "password")));
        AuthController controller = new AuthController(authenticationManager, tokenService, recruiters);

        assertEquals("Known User", controller.me(
                UsernamePasswordAuthenticationToken.authenticated("known", "", List.of())).displayName());
        assertEquals("blank", controller.me(
                UsernamePasswordAuthenticationToken.authenticated("blank", "", List.of())).displayName());
    }
}
