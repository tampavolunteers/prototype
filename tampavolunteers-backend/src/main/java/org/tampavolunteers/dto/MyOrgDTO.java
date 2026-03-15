package org.tampavolunteers.dto;

import java.time.LocalDateTime;

public record MyOrgDTO(
        Long id,
        String name,
        String description,
        String contactEmail,
        String city,
        String state,
        Boolean verified,
        String myRole,
        LocalDateTime createdAt
) {}
