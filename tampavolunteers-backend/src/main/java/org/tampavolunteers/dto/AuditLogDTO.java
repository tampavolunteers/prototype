package org.tampavolunteers.dto;

import java.time.LocalDateTime;

public record AuditLogDTO(
        Long id,
        Long adminUserId,
        String adminEmail,
        String action,
        String targetType,
        Long targetId,
        String details,
        String ipAddress,
        LocalDateTime createdAt
) {}
