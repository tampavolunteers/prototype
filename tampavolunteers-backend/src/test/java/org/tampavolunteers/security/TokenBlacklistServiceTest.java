package org.tampavolunteers.security;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies W-3: token blacklist stores and looks up jti values,
 * and purges entries that have already expired.
 */
class TokenBlacklistServiceTest {

    private final TokenBlacklistService service = new TokenBlacklistService();

    @Test
    void blacklistedToken_isRecognised() {
        String jti = "abc-123";
        Date future = new Date(System.currentTimeMillis() + 60_000);

        service.blacklist(jti, future);

        assertThat(service.isBlacklisted(jti)).isTrue();
    }

    @Test
    void unknownToken_isNotBlacklisted() {
        assertThat(service.isBlacklisted("unknown-jti")).isFalse();
    }

    @Test
    void purgeExpired_removesExpiredEntries() {
        String expiredJti = "expired-jti";
        String activeJti  = "active-jti";

        service.blacklist(expiredJti, new Date(System.currentTimeMillis() - 1)); // already expired
        service.blacklist(activeJti,  new Date(System.currentTimeMillis() + 60_000));

        service.purgeExpired();

        assertThat(service.isBlacklisted(expiredJti)).isFalse();
        assertThat(service.isBlacklisted(activeJti)).isTrue();
    }
}
