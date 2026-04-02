package org.tampavolunteers.controller;

import jakarta.servlet.http.HttpServletRequest;
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
import org.tampavolunteers.dto.UserProfileDTO;
import org.tampavolunteers.model.User;
import org.tampavolunteers.security.JwtUtil;
import org.tampavolunteers.security.OAuth.OAuthCodeStore;
import org.tampavolunteers.security.TokenBlacklistService;
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
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final OAuthCodeStore oAuthCodeStore;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Exchange a short-lived opaque code (issued after OAuth2 login) for a JWT.
     * Keeps the JWT out of the browser URL and server access logs (C-3).
     */
    @PostMapping("/oauth-token")
    public ResponseEntity<AuthenticationResponse> exchangeOAuthCode(@RequestParam String code) {
        String token = oAuthCodeStore.exchange(code);
        if (token == null) {
            return ResponseEntity.status(400).build();
        }
        String username = jwtUtil.extractUsername(token);
        org.tampavolunteers.model.User user = authService.getCurrentUser(username);
        return ResponseEntity.ok(new AuthenticationResponse(
                user.getId(), token, user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getRole().name()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        String email;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");

            if (email == null) {
                Object idObj = oauth2User.getAttribute("id");
                if (idObj == null) {
                    idObj = oauth2User.getAttribute("sub");
                }
                String providerId = idObj != null ? idObj.toString() : null;

                if (providerId != null) {
                    User user = authService.getCurrentUserByProviderId(providerId);
                    if (user != null) {
                        return ResponseEntity.ok(UserProfileDTO.from(user));
                    }
                }
            }

            if (email != null) {
                Object idObj = oauth2User.getAttribute("id");
                if (idObj == null) {
                    idObj = oauth2User.getAttribute("sub");
                }
                if (idObj != null) {
                    User user = authService.getCurrentUserByProviderId(idObj.toString());
                    if (user != null) {
                        return ResponseEntity.ok(UserProfileDTO.from(user));
                    }
                }
            }
        } else {
            return ResponseEntity.status(401).build();
        }

        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(UserProfileDTO.from(authService.getCurrentUser(email)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String jti = jwtUtil.extractJti(token);
                java.util.Date expiry = jwtUtil.extractExpiration(token);
                tokenBlacklistService.blacklist(jti, expiry);
            } catch (Exception ignored) {
                // Invalid or already-expired token — nothing to blacklist
            }
        }
        return ResponseEntity.noContent().build();
    }
}
