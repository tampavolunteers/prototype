package org.tampavolunteers.security;

import org.junit.jupiter.api.Test;
import org.tampavolunteers.security.OAuth.OAuthCodeStore;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies C-3: OAuthCodeStore issues single-use codes that map to JWTs.
 */
class OAuthCodeStoreTest {

    private final OAuthCodeStore store = new OAuthCodeStore();

    @Test
    void storedCode_exchangesForJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.test";
        String code = store.store(jwt);

        assertThat(code).isNotNull().isNotBlank();
        assertThat(store.exchange(code)).isEqualTo(jwt);
    }

    @Test
    void code_isSingleUse() {
        String code = store.store("some-jwt");

        store.exchange(code); // first use
        assertThat(store.exchange(code)).isNull(); // second use should fail
    }

    @Test
    void unknownCode_returnsNull() {
        assertThat(store.exchange("nonexistent-code")).isNull();
    }

    @Test
    void eachStore_producesDifferentCode() {
        String jwt = "same-jwt";
        String code1 = store.store(jwt);
        String code2 = store.store(jwt);
        assertThat(code1).isNotEqualTo(code2);
    }
}
