package org.tampavolunteers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tampavolunteers.model.Opportunity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Opportunity entity.
 */
@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {

    List<Opportunity> findByOrganizationId(Long organizationId);

    List<Opportunity> findByStatus(Opportunity.OpportunityStatus status);

    List<Opportunity> findByCategoryId(Long categoryId);

    @Query("SELECT o FROM Opportunity o WHERE o.status = :status AND o.startDateTime > :now ORDER BY o.startDateTime ASC")
    List<Opportunity> findUpcomingOpportunities(@Param("status") Opportunity.OpportunityStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT o FROM Opportunity o WHERE o.status = 'PUBLISHED' AND o.startDateTime > :now " +
           "AND (LOWER(o.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Opportunity> searchOpportunities(@Param("keyword") String keyword, @Param("now") LocalDateTime now);
}
