package org.tampavolunteers.security.OAuth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived, single-use code store for the OAuth2 token exchange flow (C-3).
 * The frontend receives an opaque code (not the JWT) after OAuth2 login and
 * exchanges it via POST /auth/oauth-token, keeping the JWT off the URL.
 */
@Component
public class OAuthCodeStore {

    private static final long CODE_TTL_SECONDS = 300; // 5 minutes

    private record Entry(String jwt, Instant expiresAt) {}

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    /** Store a JWT and return a single-use opaque code. */
    public String store(String jwt) {
        String code = UUID.randomUUID().toString();
        store.put(code, new Entry(jwt, Instant.now().plusSeconds(CODE_TTL_SECONDS)));
        return code;
    }

    /**
     * Exchange the code for a JWT. Returns null if the code is unknown or expired.
     * The code is consumed on first use.
     */
    public String exchange(String code) {
        Entry entry = store.remove(code);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return null;
        }
        return entry.jwt();
    }

    @Scheduled(fixedDelay = 300_000)
    public void purgeExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}
