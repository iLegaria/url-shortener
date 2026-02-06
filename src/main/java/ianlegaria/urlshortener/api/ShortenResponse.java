package ianlegaria.urlshortener.api;

public record ShortenResponse (
        String code,
        String shortUrl,
        String longUrl
){}
