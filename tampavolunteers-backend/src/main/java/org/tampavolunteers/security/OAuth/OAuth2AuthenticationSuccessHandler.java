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
import org.tampavolunteers.security.JwtTokenProvider;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        var principal = authentication.getPrincipal();

        String email = null;

        if (principal instanceof OAuth2User oauth2User) {
            // Extract email from OAuth2User attributes
            email = oauth2User.getAttribute("email");

            // If email is null (GitHub with private email), use provider ID to find user
            if (email == null) {
                Object idObj = oauth2User.getAttribute("id");
                String providerId = idObj != null ? idObj.toString() : null;
                String login = oauth2User.getAttribute("login");

                // Try to find user by provider ID or fallback email
                if (providerId != null) {
                    email = userRepository.findByProviderId(providerId)
                            .map(User::getEmail)
                            .orElse(login != null ? login + "@users.noreply.github.com" : null);
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

        String token = jwtTokenProvider.generateToken(email);
        String redirectUrl = frontendUrl + "/oauth-success?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}