package org.tampavolunteers.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom UserDetailsService implementation for loading user-specific data.
 * Grants cumulative authorities so higher roles include lower role permissions.
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
        String password = user.getPasswordHash();
        if (password == null || password.isEmpty()) {
            // This prevents "null" password encoder error while ensuring OAuth users can't login via password
            password = "{noop}OAUTH_USER_NO_PASSWORD";
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                buildAuthorities(user.getRole())
        );
    }

    private List<SimpleGrantedAuthority> buildAuthorities(User.UserRole role) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        // Cumulative: higher roles include all lower role authorities
        switch (role) {
            case SUPER_ADMIN:
                authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                // fall through
            case ADMIN:
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                // fall through
            case ORG_ADMIN:
                authorities.add(new SimpleGrantedAuthority("ROLE_ORG_ADMIN"));
                // fall through
            case VOLUNTEER:
                authorities.add(new SimpleGrantedAuthority("ROLE_VOLUNTEER"));
                break;
        }
        return authorities;
    }
}
