package ianlegaria.urlshortener.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimiter {

    private static final int LIMIT_PER_MINUTE = 60;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String PREFIX = "rl:";

    private final StringRedisTemplate redis;

    public boolean allow(String key) {
        String redisKey = PREFIX + key;

        Long count = redis.opsForValue().increment(redisKey);

        if (count != null && count == 1L) {
            redis.expire(redisKey, WINDOW);
        }

        return count != null && count <= LIMIT_PER_MINUTE;
    }

}
