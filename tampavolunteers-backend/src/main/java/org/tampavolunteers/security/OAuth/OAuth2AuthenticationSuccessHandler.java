package org.tampavolunteers.security.OAuth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.UserRepository;
import org.tampavolunteers.security.JwtUtil;
import org.springframework.security.core.userdetails.UserDetails;
import org.tampavolunteers.security.CustomUserDetailsService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;
    private final OAuthCodeStore oAuthCodeStore;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        var principal = authentication.getPrincipal();

        String email = null;

        if (principal instanceof OAuth2User oauth2User) {
            // Extract email from OAuth2User attributes
            email = oauth2User.getAttribute("email");

            // If email is null (GitHub with private email), look up by provider ID
            if (email == null) {
                Object idObj = oauth2User.getAttribute("id");
                String providerId = idObj != null ? idObj.toString() : null;

                if (providerId != null) {
                    email = userRepository.findByProviderIdAndAuthProvider(providerId, User.AuthProvider.GITHUB)
                            .map(User::getEmail)
                            .orElse(null);

                    if (email == null) {
                        email = userRepository.findByProviderIdAndAuthProvider(providerId, User.AuthProvider.GOOGLE)
                                .map(User::getEmail)
                                .orElse(null);
                    }
                }
                // No synthetic fallback email — if we still have no email the request fails below
            }
        } else if (principal instanceof User customUser) {
            email = customUser.getEmail();
        } else if (principal instanceof org.springframework.security.core.userdetails.User user) {
            email = user.getUsername();
        }

        if (email == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unable to retrieve user email");
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String token = jwtUtil.generateToken(userDetails);
        String code = oAuthCodeStore.store(token);
        response.sendRedirect(frontendUrl + "/oauth-success?code=" + code);
    }
}