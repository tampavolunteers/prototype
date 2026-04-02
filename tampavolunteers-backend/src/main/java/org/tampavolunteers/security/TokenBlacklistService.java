package org.tampavolunteers.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory blacklist for revoked JWT tokens. Entries are keyed by jti claim
 * and expire alongside the token so the map stays bounded.
 */
@Component
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Date> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String jti, Date expiry) {
        blacklist.put(jti, expiry);
    }

    public boolean isBlacklisted(String jti) {
        return blacklist.containsKey(jti);
    }

    /** Purge entries whose tokens have already expired naturally. */
    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        Date now = new Date();
        blacklist.entrySet().removeIf(e -> e.getValue().before(now));
    }
}
