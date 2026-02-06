package ianlegaria.urlshortener.store;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostgresUrlStore implements UrlStore {

    private final ShortUrlRepository repo;

    @Override
    public void save(String code, String longUrl) {
        repo.save(ShortUrl.builder()
                .code(code)
                .longUrl(longUrl)
                .build());
    }

    @Override
    public Optional<String> findLongUrl(String code) {
        return repo.findByCode(code).map(ShortUrl::getLongUrl);
    }

    @Override
    public boolean existsByCode(String code) {
        return repo.existsByCode(code);
    }

    @Override
    public Optional<String> findCodeByLongUrl(String longUrl) {
        return repo.findByLongUrl(longUrl).map(ShortUrl::getCode);
    }
}
