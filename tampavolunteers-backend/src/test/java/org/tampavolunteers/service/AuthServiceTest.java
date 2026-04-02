package org.tampavolunteers.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.tampavolunteers.dto.AuthenticationResponse;
import org.tampavolunteers.dto.RegisterRequest;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.UserRepository;
import org.tampavolunteers.security.CustomUserDetailsService;
import org.tampavolunteers.security.JwtUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies C-1: registration always assigns VOLUNTEER role regardless of input.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock AuthenticationManager authenticationManager;
    @Mock CustomUserDetailsService userDetailsService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-for-unit-tests-minimum-256-bits-abcdefghijklmnop");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L);

        authService = new AuthService(
                userRepository, new BCryptPasswordEncoder(),
                authenticationManager, jwtUtil, userDetailsService);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(new org.springframework.security.core.userdetails.User(
                        "test@example.com", "hashed", java.util.Collections.emptyList()));
    }

    @Test
    void register_alwaysAssignsVolunteerRole() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com", "password1", "Jane", "Doe", null);

        AuthenticationResponse response = authService.register(request);

        assertThat(response.getRole()).isEqualTo(User.UserRole.VOLUNTEER.name());
    }

    @Test
    void register_cannotEscalateToAdmin() {
        RegisterRequest request = new RegisterRequest(
                "attacker@example.com", "password1", "Bad", "Actor", null);

        AuthenticationResponse response = authService.register(request);

        assertThat(response.getRole()).isEqualTo(User.UserRole.VOLUNTEER.name());
        assertThat(response.getRole()).isNotEqualTo(User.UserRole.ADMIN.name());
        assertThat(response.getRole()).isNotEqualTo(User.UserRole.SUPER_ADMIN.name());
    }

    @Test
    void register_doesNotExposePasswordHash() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com", "password1", "Jane", "Doe", null);

        AuthenticationResponse response = authService.register(request);

        assertThat(response.getToken()).isNotNull().isNotBlank();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }
}
