package ianlegaria.urlshortener.store;

import java.util.Optional;

public interface UrlStore {
    void save(String code, String longUrl);
    Optional<String> findLongUrl(String code);
    boolean existsByCode(String code);
    Optional<String> findCodeByLongUrl(String longUrl);
}
