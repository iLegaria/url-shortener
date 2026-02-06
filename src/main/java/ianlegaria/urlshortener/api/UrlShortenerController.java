package ianlegaria.urlshortener.api;

import ianlegaria.urlshortener.service.UrlNotFoundException;
import ianlegaria.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @PostMapping("/api/shorten")
    public ShortenResponse shorten(@Valid @RequestBody ShortenRequest request, HttpServletRequest http) {
        String code = urlShortenerService.shorten(request.url());
        String baseUrl = http.getScheme() + "://" + http.getServerName() + ":" + http.getServerPort();
        String shortUrl = baseUrl + "/" + code;

        log.info("Shortened url to code={} shortUrl={}", code, shortUrl);
        return new ShortenResponse(code, shortUrl, request.url());
    }

    @GetMapping("{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String longUrl = urlShortenerService.resolve(code);
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, longUrl)
                .build();
    }

    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<String> notFound(UrlNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

}
