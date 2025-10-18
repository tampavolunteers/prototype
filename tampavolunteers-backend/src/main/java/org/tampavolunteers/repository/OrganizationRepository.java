package org.tampavolunteers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tampavolunteers.model.Organization;

import java.util.List;

/**
 * Repository for Organization entity.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findByUserId(Long userId);

    List<Organization> findByVerified(Boolean verified);
}
