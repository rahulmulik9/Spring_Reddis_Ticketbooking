package com.rahul.ticketbooking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    // No token, or an invalid/expired/blacklisted one -> 401
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        write(response, 401, "Unauthorized", "Missing or invalid token");
    }

    // Valid token but wrong role -> 403
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        write(response, 403, "Forbidden", "You do not have permission");
    }

    private void write(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}