package com.shieldgate.filter;

import com.shieldgate.dto.ThreatEvent;
import com.shieldgate.service.JwtService;
import com.shieldgate.service.ThreatEventPublisher;
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
    private final ThreatEventPublisher threatEventPublisher;

    public JwtAuthenticationFilter(JwtService jwtService, ThreatEventPublisher threatEventPublisher) {
        this.jwtService = jwtService;
        this.threatEventPublisher = threatEventPublisher;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();

        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            try {
                threatEventPublisher.publish(new ThreatEvent(
                        "MISSING_JWT", httpRequest.getRemoteAddr(), "unknown", path));
            } catch (Exception publishException) {
                System.err.println("WARNING: Threat publish failed: " + publishException.getMessage());
            }
            sendError(httpResponse, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        try { 
            String username = jwtService.validateToken(token);
            httpRequest.setAttribute("username", username);
            chain.doFilter(request, response);
        } catch (Exception e) {
            try {
                threatEventPublisher.publish(new ThreatEvent(
                        "INVALID_JWT", httpRequest.getRemoteAddr(), "unknown", path));
            } catch (Exception publishException) {
                System.err.println("WARNING: Threat publish failed: " + publishException.getMessage());
            }
        sendError(httpResponse, "Invalid or expired token");
        }
    }

    private boolean isPublicPath(String path) {
        return path.equals("/health")
                || path.startsWith("/auth/")
                || path.equals("/")
                || path.startsWith("/dashboard/")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".ico");
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
