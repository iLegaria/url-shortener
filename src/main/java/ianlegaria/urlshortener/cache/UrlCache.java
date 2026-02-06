package ianlegaria.urlshortener.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UrlCache {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "short:";
    private static final Duration TTL_BASE = Duration.ofHours(24);
    private static final Duration TTL_JITTER_MAX = Duration.ofHours(1);
    public static final String NEGATIVE_MARKER = "__NULL__";
    private static final Duration NEGATIVE_CACHE_TTL = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;

    public Optional<String> getLongUrl(String code) {
        String value = redis.opsForValue().get(PREFIX + code);

        if (value == null) return Optional.empty();

        if (value.equals(NEGATIVE_MARKER)) return Optional.of(NEGATIVE_MARKER);

        return Optional.of(value);
    }

    public void putLongUrl(String code, String longUrl) {
        Duration ttl = TTL_BASE.plusSeconds(RANDOM.nextLong(TTL_JITTER_MAX.getSeconds() + 1));
        redis.opsForValue().set(PREFIX + code, longUrl, ttl);
    }

    public void putNegative(String code) {
        redis.opsForValue().set(PREFIX + code, NEGATIVE_MARKER, NEGATIVE_CACHE_TTL);
    }

}
