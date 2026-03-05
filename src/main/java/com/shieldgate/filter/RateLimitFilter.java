package com.shieldgate.filter;

import com.shieldgate.dto.ThreatEvent;
import com.shieldgate.service.RateLimiterService;
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
@Order(2)
public class RateLimitFilter implements Filter {

    private final RateLimiterService rateLimiterService;
    private final ThreatEventPublisher threatEventPublisher;

    public RateLimitFilter(RateLimiterService rateLimiterService, ThreatEventPublisher threatEventPublisher) {
        this.rateLimiterService = rateLimiterService;
        this.threatEventPublisher = threatEventPublisher;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String key = getClientKey(httpRequest);

        if (rateLimiterService.isRateLimited(key)) {
            threatEventPublisher.publish(new ThreatEvent(
                    "RATE_LIMIT_EXCEEDED", httpRequest.getRemoteAddr(), key, httpRequest.getRequestURI()));
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Rate limit exceeded. Try again later.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientKey(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        if (username != null) {
            return username;
        }
        return request.getRemoteAddr();
    }
}
