package com.shieldgate.filter;

import com.shieldgate.dto.ThreatEvent;
import com.shieldgate.service.JwtService;
import com.shieldgate.service.ThreatEventPublisher;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private ThreatEventPublisher threatEventPublisher;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, threatEventPublisher);
    }

    @Test
    void testPublicPathBypassesAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService, threatEventPublisher);
    }

    @Test
    void testMissingAuthorizationHeaderReturnsUnauthorized() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(threatEventPublisher, times(1)).publish(any(ThreatEvent.class));
        verifyNoInteractions(filterChain);
    }

    @Test
    void testValidJwtTokenSetsUsernameAndProceeds() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resource");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validateToken("valid-token")).thenReturn("alice");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals("alice", request.getAttribute("username"));
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(threatEventPublisher);
    }

    @Test
    void testInvalidJwtTokenReturnsUnauthorized() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/resource");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.validateToken("invalid-token")).thenThrow(new JwtException("Invalid token"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(threatEventPublisher, times(1)).publish(any(ThreatEvent.class));
        verifyNoInteractions(filterChain);
    }
}
