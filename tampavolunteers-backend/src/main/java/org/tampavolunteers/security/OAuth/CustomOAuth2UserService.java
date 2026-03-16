package org.tampavolunteers.security.OAuth;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.tampavolunteers.model.User;
import org.tampavolunteers.model.User.AuthProvider;
import org.tampavolunteers.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = null;
        String firstName = null;
        String lastName = null;
        String providerId = null;

        if ("google".equalsIgnoreCase(registrationId)) {

            providerId = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            String[] fullName = splitName((String) attributes.get("name"));
            firstName = fullName[0];
            lastName = fullName[1]; // haha no line break

        } else if ("github".equalsIgnoreCase(registrationId)) {

            providerId = String.valueOf(attributes.get("id"));
            email = (String) attributes.get("email");

            // GitHub often doesn't include email in the main user response
            // even when it's public. We need to fetch it from the emails endpoint
            if (email == null || email.isEmpty()) {
                email = fetchGithubPrimaryEmail(userRequest);
            }

            // If still null after API call, use the fallback
            if (email == null || email.isEmpty()) {
                email = fetchGithubEmailFallback(attributes);
            }

            String[] fullName = splitName((String) attributes.get("name"));
            if (fullName == null) {
                firstName = (String) attributes.get("login");
                lastName = "";
            } else {
                firstName = fullName[0];
                lastName = fullName[1];
            }

        }

        if (email == null) {
            throw new RuntimeException("No email provided by OAuth2 provider.");
        }

        // Determine the auth provider enum
        AuthProvider authProvider = AuthProvider.valueOf(registrationId.toUpperCase());

        // First, try to find user by provider ID + auth provider (most specific)
        Optional<User> existingUser = Optional.empty();
        if (providerId != null) {
            existingUser = userRepository.findByProviderIdAndAuthProvider(providerId, authProvider);
        }

        // If not found by provider ID, try to find by email (for existing users or
        // account linking)
        if (existingUser.isEmpty()) {
            existingUser = userRepository.findByEmail(email);
        }

        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update the auth provider and provider ID (in case user previously used
            // different method)
            user.setAuthProvider(authProvider);
            user.setProviderId(providerId);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        } else {
            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName != null ? firstName : "");
            user.setLastName(lastName != null ? lastName : "");
            user.setRole(User.UserRole.VOLUNTEER);
            user.setAuthProvider(authProvider);
            user.setProviderId(providerId);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }

        return oAuth2User;
    }

    private String[] splitName(String name) {
        String firstName, lastName;
        if (name != null) {
            String[] parts = name.split(" ", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : "";
        } else {
            return null;
        }
        return new String[] { firstName, lastName };
    }

    // Why is this even a thing, lul
    private String fetchGithubEmailFallback(Map<String, Object> attributes) {
        Object login = attributes.get("login");
        if (login != null) {
            return login + "@users.noreply.github.com";
        }
        return null;
    }

    /**
     * Fetch the primary email from GitHub's /user/emails endpoint
     * GitHub doesn't always include email in the main user response,
     * even if the user has a public primary email set.
     */
    private String fetchGithubPrimaryEmail(OAuth2UserRequest userRequest) {
        try {
            String accessToken = userRequest.getAccessToken().getTokenValue();

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });

            List<Map<String, Object>> emails = response.getBody();

            if (emails != null && !emails.isEmpty()) {
                // First, try to find the primary email
                for (Map<String, Object> emailData : emails) {
                    Boolean primary = (Boolean) emailData.get("primary");
                    Boolean verified = (Boolean) emailData.get("verified");
                    String email = (String) emailData.get("email");

                    if (primary != null && primary && verified != null && verified && email != null) {
                        return email;
                    }
                }

                // If no primary verified email found, use any verified email
                for (Map<String, Object> emailData : emails) {
                    Boolean verified = (Boolean) emailData.get("verified");
                    String email = (String) emailData.get("email");

                    if (verified != null && verified && email != null) {
                        return email;
                    }
                }

                // Last resort: use any email (even unverified)
                for (Map<String, Object> emailData : emails) {
                    String email = (String) emailData.get("email");
                    if (email != null) {
                        return email;
                    }
                }
            }
        } catch (Exception e) {
            // Log the error but don't fail - we'll fall back to the noreply email
            System.err.println("Failed to fetch GitHub email: " + e.getMessage());
        }

        return null;
    }
}
