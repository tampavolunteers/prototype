package org.tampavolunteers.dto;

import org.tampavolunteers.model.UserStatus;

import java.time.LocalDateTime;

public record PublicProfileDTO(
        Long id,
        String firstName,
        String lastName,
        String bio,
        String avatarUrl,
        UserStatus userStatus,
        LocalDateTime memberSince
) {}
