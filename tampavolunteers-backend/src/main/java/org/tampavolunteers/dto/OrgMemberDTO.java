package org.tampavolunteers.dto;

import java.time.LocalDateTime;

public record OrgMemberDTO(
        Long id,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String role,
        LocalDateTime joinedAt,
        String invitedByEmail
) {}
