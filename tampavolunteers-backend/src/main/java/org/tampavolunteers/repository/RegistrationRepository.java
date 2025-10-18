package org.tampavolunteers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tampavolunteers.model.Registration;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Registration entity.
 */
@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByUserId(Long userId);

    List<Registration> findByOpportunityId(Long opportunityId);

    Optional<Registration> findByOpportunityIdAndUserId(Long opportunityId, Long userId);

    List<Registration> findByUserIdAndStatus(Long userId, Registration.RegistrationStatus status);

    long countByOpportunityIdAndStatus(Long opportunityId, Registration.RegistrationStatus status);
}
