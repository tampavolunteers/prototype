package org.tampavolunteers.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.tampavolunteers.dto.OpportunityDTO;
import org.tampavolunteers.exception.NotFoundException;
import org.tampavolunteers.model.Opportunity;
import org.tampavolunteers.repository.OpportunityRepository;
import org.tampavolunteers.repository.OpportunitySpec;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;

    public Page<OpportunityDTO> getOpportunities(
            String keyword,
            Long categoryId,
            Long organizationId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size) {

        Specification<Opportunity> spec = Specification
                .where(OpportunitySpec.isPublished())
                .and(OpportunitySpec.upcomingFrom(startDate != null ? startDate : LocalDateTime.now()));

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(OpportunitySpec.keywordMatches(keyword));
        }
        if (categoryId != null) {
            spec = spec.and(OpportunitySpec.hasCategory(categoryId));
        }
        if (organizationId != null) {
            spec = spec.and(OpportunitySpec.hasOrganization(organizationId));
        }
        if (endDate != null) {
            spec = spec.and(OpportunitySpec.startsBefore(endDate));
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by("startDateTime").ascending());
        return opportunityRepository.findAll(spec, pageable).map(this::toDTO);
    }

    public OpportunityDTO getOpportunityById(Long id) {
        Opportunity opp = opportunityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Opportunity not found: " + id));
        return toDTO(opp);
    }

    private OpportunityDTO toDTO(Opportunity o) {
        return new OpportunityDTO(
                o.getId(),
                o.getTitle(),
                o.getDescription(),
                o.getStartDateTime(),
                o.getEndDateTime(),
                o.getStreet(),
                o.getCity(),
                o.getState(),
                o.getZip(),
                o.getSlotsAvailable(),
                o.getSlotsFilled(),
                o.getStatus().name(),
                o.getOrganization() != null ? o.getOrganization().getId() : null,
                o.getOrganization() != null ? o.getOrganization().getName() : null,
                o.getCategory() != null ? o.getCategory().getId() : null,
                o.getCategory() != null ? o.getCategory().getName() : null,
                o.getCreatedAt()
        );
    }
}
