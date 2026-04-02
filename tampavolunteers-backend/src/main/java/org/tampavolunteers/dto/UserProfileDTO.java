package org.tampavolunteers.dto;

import org.tampavolunteers.model.User;
import org.tampavolunteers.model.UserStatus;

import java.time.LocalDateTime;

public record UserProfileDTO(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String bio,
        String avatarUrl,
        Boolean isPublic,
        UserStatus userStatus,
        User.UserRole role,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    public static UserProfileDTO from(User user) {
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getIsPublic(),
                user.getUserStatus(),
                user.getRole(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
