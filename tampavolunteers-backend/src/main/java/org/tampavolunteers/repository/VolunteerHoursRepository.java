package org.tampavolunteers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tampavolunteers.model.VolunteerHours;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for VolunteerHours entity.
 */
@Repository
public interface VolunteerHoursRepository extends JpaRepository<VolunteerHours, Long> {

    List<VolunteerHours> findByUserId(Long userId);

    List<VolunteerHours> findByOpportunityId(Long opportunityId);

    List<VolunteerHours> findByRegistrationId(Long registrationId);

    @Query("SELECT SUM(vh.hoursLogged) FROM VolunteerHours vh WHERE vh.user.id = :userId")
    BigDecimal getTotalHoursByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(vh.hoursLogged) FROM VolunteerHours vh WHERE vh.user.id = :userId AND vh.verifiedBy IS NOT NULL")
    BigDecimal getVerifiedHoursByUserId(@Param("userId") Long userId);
}
