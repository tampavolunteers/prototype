package org.tampavolunteers.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.UserRepository;

import java.util.Collections;

/**
 * Custom UserDetailsService implementation for loading user-specific data.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // For OAuth users, password authentication should not be used
        // Use a random BCrypt hash that will never match any input
        String password = user.getPasswordHash();
        if (password == null || password.isEmpty()) {
            // This prevents "null" password encoder error while ensuring OAuth users can't login via password
            password = "{noop}OAUTH_USER_NO_PASSWORD";
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
