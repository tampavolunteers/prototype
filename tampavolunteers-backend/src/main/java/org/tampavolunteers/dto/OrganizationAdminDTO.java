package org.tampavolunteers.dto;

import java.time.LocalDateTime;

public record OrganizationAdminDTO(
        Long id,
        String name,
        String description,
        String website,
        String contactEmail,
        String contactPhone,
        String city,
        String state,
        Boolean verified,
        Boolean seeded,
        String ownerEmail,
        LocalDateTime createdAt
) {}
