package com.wetube.wetube_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException e)
            throws IOException {
        log.error("Unauthorized error: {}", e.getMessage());
        response.setContentType("application/json");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        Map<String, Object> errorResponse = Map.of(
                "error", "Unauthorized",
                "error_code", "unauthenticated",
                "message", e.getMessage() != null ? e.getMessage() : "Authentication failed"
        );
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

