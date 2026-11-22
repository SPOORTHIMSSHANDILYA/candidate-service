package com.recruit.candidate_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Returns 401 / 403 in the same JSON shape as every other error: {"error": "..."}.
 * It sends no "WWW-Authenticate" header: the React login page handles sign-in.
 */
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    // 401: no token, or the token is invalid / expired
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        boolean sentToken = request.getHeader(HttpHeaders.AUTHORIZATION) != null;
        write(response, HttpServletResponse.SC_UNAUTHORIZED,
                sentToken ? "Invalid or expired token" : "Authentication required");
    }

    // 403: signed in, but not allowed (no roles yet; here for when roles are added)
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, "Access denied");
    }

    private static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // fixed messages only, so no JSON escaping is needed
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
