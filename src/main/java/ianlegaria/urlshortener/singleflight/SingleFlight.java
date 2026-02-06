package ianlegaria.urlshortener.singleflight;

import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SingleFlight {

    private final ConcurrentHashMap<String, CompletableFuture<String>> inFlight = new ConcurrentHashMap<>();

    public CompletableFuture<String> exeute(String key, Callable<String> loader) {
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture<String> existing = inFlight.putIfAbsent(key, future);

        if (existing != null) {
            return existing;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String result = loader.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                inFlight.remove(key);
            }
        });

        return future;
    }
}
