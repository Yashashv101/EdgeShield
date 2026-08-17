package com.shieldgate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(redisTemplate, 100, 60);
    }

    @Test
    void testIsNotRateLimitedWhenUnderLimit() {
        // Lua returns 0 -> allowed
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(0L);

        boolean rateLimited = rateLimiterService.isRateLimited("user-123");

        assertFalse(rateLimited);
    }

    @Test
    void testIsRateLimitedWhenLimitExceeded() {
        // Lua returns 1 -> blocked
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        boolean rateLimited = rateLimiterService.isRateLimited("user-123");

        assertTrue(rateLimited);
    }

    @Test
    void testValidateConfigThrowsWhenMaxRequestsInvalid() {
        RateLimiterService service = new RateLimiterService(redisTemplate, 0, 60);
        assertThrows(IllegalStateException.class, service::validateConfig);
    }

    @Test
    void testValidateConfigThrowsWhenWindowSecondsInvalid() {
        RateLimiterService service = new RateLimiterService(redisTemplate, 10, -5);
        assertThrows(IllegalStateException.class, service::validateConfig);
    }
}
