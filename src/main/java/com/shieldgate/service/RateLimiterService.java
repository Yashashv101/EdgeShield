package com.shieldgate.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
public class RateLimiterService {
    private final StringRedisTemplate redisTemplate;
    private final int maxRequests;
    private final int windowSeconds;

    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>(
            """
            local key       = KEYS[1]
            local now       = tonumber(ARGV[1])
            local window    = tonumber(ARGV[2])
            local limit     = tonumber(ARGV[3])
            local windowMs  = window * 1000

            -- Evict timestamps that are outside the rolling window
            redis.call('ZREMRANGEBYSCORE', key, 0, now - windowMs)

            local count = redis.call('ZCARD', key)

            if count < limit then
                -- Use a Redis-generated sequence number as a unique tiebreaker
                -- so two requests landing in the same millisecond never collide.
                local seq = redis.call('INCR', key .. ':seq')
                redis.call('PEXPIRE', key .. ':seq', windowMs)
                redis.call('ZADD', key, now, now .. '-' .. seq)
                redis.call('PEXPIRE', key, windowMs)
                return 0
            end

            return 1
            """,
            Long.class
    );

    public RateLimiterService(StringRedisTemplate redisTemplate,
                              @Value("${ratelimit.max-requests}") int maxRequests,
                              @Value("${ratelimit.window-seconds}") int windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @PostConstruct
    void validateConfig() {
        if (maxRequests <= 0) {
            throw new IllegalStateException("ratelimit.max-requests must be > 0, got: " + maxRequests);
        }
        if (windowSeconds <= 0) {
            throw new IllegalStateException("ratelimit.window-seconds must be > 0, got: " + windowSeconds);
        }
    }

    /**
     * Returns true if the given key has exceeded its rate limit.
     */
    public boolean isRateLimited(String key) {
        long nowMs = System.currentTimeMillis();
        Long result = redisTemplate.execute(
                SLIDING_WINDOW_SCRIPT,
                List.of("rate:" + key),
                String.valueOf(nowMs),
                String.valueOf(windowSeconds),
                String.valueOf(maxRequests)
        );
        return Long.valueOf(1L).equals(result);
    }
}