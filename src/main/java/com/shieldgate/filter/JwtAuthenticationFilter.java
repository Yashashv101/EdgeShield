package com.shieldgate.filter;

import com.shieldgate.service.JwtService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class JwtAuthenticationFilter implements Filter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();

        // Skip public paths — no token needed
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Extract the Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(httpResponse, "Missing or invalid Authorization header");
            return;
        }

        // Validate the token
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        try {
            String username = jwtService.validateToken(token);
            // Store username in request so downstream code can use it
            httpRequest.setAttribute("username", username);
            chain.doFilter(request, response);
        } catch (Exception e) {
            sendError(httpResponse, "Invalid or expired token");
        }
    }

    private boolean isPublicPath(String path) {
        return path.equals("/health") || path.startsWith("/auth/");
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
