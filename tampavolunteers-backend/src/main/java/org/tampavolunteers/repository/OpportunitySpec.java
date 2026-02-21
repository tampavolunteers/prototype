package org.tampavolunteers.repository;

import org.springframework.data.jpa.domain.Specification;
import org.tampavolunteers.model.Opportunity;

import java.time.LocalDateTime;

public class OpportunitySpec {

    private OpportunitySpec() {}

    public static Specification<Opportunity> isPublished() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), Opportunity.OpportunityStatus.PUBLISHED);
    }

    public static Specification<Opportunity> upcomingFrom(LocalDateTime from) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("startDateTime"), from);
    }

    public static Specification<Opportunity> keywordMatches(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Opportunity> hasCategory(Long categoryId) {
        return (root, query, cb) ->
                cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Opportunity> hasOrganization(Long organizationId) {
        return (root, query, cb) ->
                cb.equal(root.get("organization").get("id"), organizationId);
    }

    public static Specification<Opportunity> startsBefore(LocalDateTime date) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("startDateTime"), date);
    }
}
