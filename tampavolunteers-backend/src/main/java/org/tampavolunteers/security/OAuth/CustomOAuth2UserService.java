package org.tampavolunteers.security.OAuth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.tampavolunteers.model.User;
import org.tampavolunteers.model.User.AuthProvider;
import org.tampavolunteers.repository.UserRepository;

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
            firstName = fullName[0]; lastName = fullName[1]; // haha no line break

        } else if ("github".equalsIgnoreCase(registrationId)) {

            providerId = String.valueOf(attributes.get("id"));
            email = (String) attributes.get("email");

            if (email == null) {
                email = fetchGithubEmailFallback(attributes);
            }

            String[] fullName = splitName((String) attributes.get("name"));
            if (fullName==null){
                firstName = (String) attributes.get("login");
                lastName = "";
            } else {
                firstName = fullName[0]; lastName = fullName[1];
            }

        }

        if (email == null) {
            throw new RuntimeException("No email provided by OAuth2 provider.");
        }

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.setAuthProvider(AuthProvider.valueOf(registrationId.toUpperCase()));
            user.setProviderId(providerId);
        } else {
            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName != null ? firstName : "");
            user.setLastName(lastName != null ? lastName : "");
            user.setAuthProvider(AuthProvider.valueOf(registrationId.toUpperCase()));
            user.setProviderId(providerId);
            userRepository.save(user);
        }

        return oAuth2User;
    }

    private String[] splitName(String name) {
        String firstName = "";
        String lastName = "";
        if (name != null) {
            String[] parts = name.split(" ", 2);
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[1] : "";
        } else {
            return null;
        }
        return new String[]{firstName, lastName};
    }

    // Why is this even a thing, lul
    private String fetchGithubEmailFallback(Map<String, Object> attributes) {
        Object login = attributes.get("login");
        if (login != null) {
            return login + "@users.noreply.github.com";
        }
        return null;
    }
}
