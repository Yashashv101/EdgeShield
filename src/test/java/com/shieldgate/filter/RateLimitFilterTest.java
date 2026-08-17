package com.shieldgate.filter;

import com.shieldgate.dto.ThreatEvent;
import com.shieldgate.service.RateLimiterService;
import com.shieldgate.service.ThreatEventPublisher;
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
class RateLimitFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private ThreatEventPublisher threatEventPublisher;

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter(rateLimiterService, threatEventPublisher);
    }

    @Test
    void testRequestAllowedProceeds() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.setAttribute("username", "bob");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.isRateLimited("bob")).thenReturn(false);

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(threatEventPublisher);
    }

    @Test
    void testRequestRateLimitedReturns429AndPublishesThreat() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.setRequestURI("/api/submit");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimiterService.isRateLimited("10.0.0.1")).thenReturn(true);

        rateLimitFilter.doFilter(request, response, filterChain);

        assertEquals(429, response.getStatus());
        verify(threatEventPublisher, times(1)).publish(any(ThreatEvent.class));
        verifyNoInteractions(filterChain);
    }
}
