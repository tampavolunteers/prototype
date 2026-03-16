package org.tampavolunteers.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.tampavolunteers.dto.AuthenticationRequest;
import org.tampavolunteers.dto.AuthenticationResponse;
import org.tampavolunteers.dto.RegisterRequest;
import org.tampavolunteers.model.User;
import org.tampavolunteers.service.AuthService;

/**
 * Controller for authentication endpoints.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@ConditionalOnWebApplication
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        String email;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof OAuth2User oauth2User) {
            // Try to get email from OAuth2 attributes
            email = oauth2User.getAttribute("email");

            // If email not available, try to find user by provider ID
            if (email == null) {
                // GitHub uses "id", Google uses "sub"
                Object idObj = oauth2User.getAttribute("id");
                if (idObj == null) {
                    idObj = oauth2User.getAttribute("sub");
                }
                String providerId = idObj != null ? idObj.toString() : null;

                if (providerId != null) {
                    User user = authService.getCurrentUserByProviderId(providerId);
                    if (user != null) {
                        return ResponseEntity.ok(user);
                    }
                }

                // Fallback to login@github email for GitHub
                String login = oauth2User.getAttribute("login");
                if (login != null) {
                    email = login + "@users.noreply.github.com";
                }
            }

            // If we have email, also try provider ID lookup first (more reliable)
            if (email != null) {
                Object idObj = oauth2User.getAttribute("id");
                if (idObj == null) {
                    idObj = oauth2User.getAttribute("sub");
                }
                if (idObj != null) {
                    User user = authService.getCurrentUserByProviderId(idObj.toString());
                    if (user != null) {
                        return ResponseEntity.ok(user);
                    }
                }
            }
        } else {
            return ResponseEntity.status(401).build();
        }

        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        User user = authService.getCurrentUser(email);
        return ResponseEntity.ok(user);
    }
}
