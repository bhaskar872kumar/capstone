package com.healthdekho.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 403 when a request is refused: missing/invalid CSRF token, or not allowed. */
@Component
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemResponseWriter writer;

    public ProblemAccessDeniedHandler(ProblemResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (accessDeniedException instanceof CsrfException) {
            writer.write(request, response, HttpStatus.FORBIDDEN, "CSRF_TOKEN_INVALID",
                    "Your session security check failed. Refresh the page and try again.");
            return;
        }
        writer.write(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You don't have access to this.");
    }
}
