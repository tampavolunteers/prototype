package org.tampavolunteers.dto;

import java.time.LocalDateTime;

public record OpportunityDTO(
        Long id,
        String title,
        String description,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String street,
        String city,
        String state,
        String zip,
        Integer slotsAvailable,
        Integer slotsFilled,
        String status,
        Long organizationId,
        String organizationName,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt
) {}
