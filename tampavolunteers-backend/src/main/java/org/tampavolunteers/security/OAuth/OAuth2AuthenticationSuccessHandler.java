package org.tampavolunteers.security.OAuth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.UserRepository;
import org.tampavolunteers.security.JwtUtil;
import org.springframework.security.core.userdetails.UserDetails;
import org.tampavolunteers.security.CustomUserDetailsService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

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

            // If email is null (GitHub with private email), try to find user by provider ID
            if (email == null) {
                Object idObj = oauth2User.getAttribute("id");
                String providerId = idObj != null ? idObj.toString() : null;
                String login = oauth2User.getAttribute("login");

                // Determine which provider this is (GitHub in this case since Google always has
                // email)
                // We need to determine the auth provider to query correctly
                if (providerId != null) {
                    // Try GitHub first (since we're here because email was null)
                    email = userRepository.findByProviderIdAndAuthProvider(providerId, User.AuthProvider.GITHUB)
                            .map(User::getEmail)
                            .orElse(null);

                    // If not found with GitHub, try Google (shouldn't happen, but defensive)
                    if (email == null) {
                        email = userRepository.findByProviderIdAndAuthProvider(providerId, User.AuthProvider.GOOGLE)
                                .map(User::getEmail)
                                .orElse(login != null ? login + "@users.noreply.github.com" : null);
                    }
                } else if (login != null) {
                    email = login + "@users.noreply.github.com";
                }
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

        // The JWT is the credential from here on; drop the OAuth2 login session so its
        // JSESSIONID (sent same-origin through the /api proxy) can't shadow the JWT's roles.
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        String redirectUrl = frontendUrl + "/oauth-success?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}