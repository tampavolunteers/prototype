package org.tampavolunteers.dto;

import org.tampavolunteers.model.User;
import org.tampavolunteers.model.UserStatus;

import java.time.LocalDateTime;

public record UserSummaryDTO(
        Long id,
        String email,
        String firstName,
        String lastName,
        User.UserRole role,
        UserStatus userStatus,
        Boolean isPublic,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {}
