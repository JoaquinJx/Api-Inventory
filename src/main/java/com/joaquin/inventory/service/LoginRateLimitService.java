package com.joaquin.inventory.service;

import com.joaquin.inventory.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimitService {

    private record AttemptBucket(int attempts, long firstAttemptEpochMs) {
    }

    private final int maxAttempts;
    private final long windowMs;
    private final Map<String, AttemptBucket> attemptsByKey = new ConcurrentHashMap<>();

    public LoginRateLimitService(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.window-ms:60000}") long windowMs
    ) {
        this.maxAttempts = maxAttempts;
        this.windowMs = windowMs;
    }

    public void assertAllowed(String key) {
        AttemptBucket bucket = attemptsByKey.get(key);
        if (bucket == null) {
            return;
        }

        long now = Instant.now().toEpochMilli();
        if (now - bucket.firstAttemptEpochMs() > windowMs) {
            attemptsByKey.remove(key);
            return;
        }

        if (bucket.attempts() >= maxAttempts) {
            throw new TooManyRequestsException("Too many login attempts. Try again later.");
        }
    }

    public void onFailure(String key) {
        long now = Instant.now().toEpochMilli();
        attemptsByKey.compute(key, (k, current) -> {
            if (current == null || now - current.firstAttemptEpochMs() > windowMs) {
                return new AttemptBucket(1, now);
            }
            return new AttemptBucket(current.attempts() + 1, current.firstAttemptEpochMs());
        });
    }

    public void onSuccess(String key) {
        attemptsByKey.remove(key);
    }
}