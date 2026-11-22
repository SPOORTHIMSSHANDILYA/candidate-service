package com.recruit.candidate_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs inside the security chain, right after the JWT is checked, and puts the username into the
 * logging context (MDC). Every log line of the request then shows who made it.
 * RequestLoggingFilter clears the MDC when the request ends.
 * Not a Spring bean on purpose: as a bean it would also be registered outside the security chain.
 */
class UserMdcFilter extends OncePerRequestFilter {

    static final String MDC_USER = "user";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String username = CurrentUser.username();
        if (!CurrentUser.SYSTEM.equals(username)) {
            MDC.put(MDC_USER, username);
        }
        chain.doFilter(request, response);
    }
}
