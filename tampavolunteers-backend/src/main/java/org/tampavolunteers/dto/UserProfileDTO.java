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
) {}
