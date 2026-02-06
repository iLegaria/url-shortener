package ianlegaria.urlshortener.service;

import ianlegaria.urlshortener.cache.UrlCache;
import ianlegaria.urlshortener.singleflight.SingleFlight;
import ianlegaria.urlshortener.store.UrlStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;

import static ianlegaria.urlshortener.cache.UrlCache.NEGATIVE_MARKER;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 7;

    private final SecureRandom random = new SecureRandom();

    private final UrlStore store;
    private final UrlCache cache;
    private final SingleFlight singleFlight;

    public String shorten(String longUrl) {
        // Check if existing code exists for the long URL
        var existing = store.findCodeByLongUrl(longUrl);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Retry logic with max attempts
        int maxAttempts = 10;

        // If not found, generate a unique code
        for (int i = 0; i < maxAttempts; i++) {
            String code = generateCode();

            if (store.existsByCode(code)) {
                continue;
            }

            try {
                store.save(code, longUrl);
                return code;
            } catch (DataIntegrityViolationException ex) {
                log.warn("Collision detected at DB level for code={}", code);
            }
        }

        throw new IllegalStateException("Could not generate unique short code after retries");
    }

    public String resolve(String code) {

        var cached = cache.getLongUrl(code);

        if (cached.isPresent()) {
            if ("__NULL__".equals(cached.get())) {
                throw new UrlNotFoundException(code);
            }
            return cached.get();
        }

        try {
            return singleFlight.exeute(code, () -> {

                var cachedAgain = cache.getLongUrl(code);
                if (cachedAgain.isPresent()) {
                    if ("__NULL__".equals(cachedAgain.get())) {
                        throw new UrlNotFoundException(code);
                    }
                    return cachedAgain.get();
                }

                var longUrlOpt = store.findLongUrl(code);

                if (longUrlOpt.isEmpty()) {
                    cache.putNegative(code);
                    throw new UrlNotFoundException(code);
                }

                cache.putLongUrl(code, longUrlOpt.get());
                return longUrlOpt.get();

            }).get();

        } catch (ExecutionException e) {
            if (e.getCause() instanceof UrlNotFoundException ex) {
                throw ex;
            }
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
