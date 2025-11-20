package org.tampavolunteers.security.OAuth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.tampavolunteers.model.User;
import org.tampavolunteers.security.JwtTokenProvider;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

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

        if (principal instanceof User customUser) {
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