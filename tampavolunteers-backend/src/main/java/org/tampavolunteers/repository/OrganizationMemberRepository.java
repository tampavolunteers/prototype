package org.tampavolunteers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tampavolunteers.model.OrganizationMember;
import org.tampavolunteers.model.OrganizationMember.OrgMemberRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    List<OrganizationMember> findByOrganizationId(Long orgId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long orgId, Long userId);

    List<OrganizationMember> findByUserId(Long userId);

    boolean existsByOrganizationIdAndUserIdAndRoleIn(Long orgId, Long userId, List<OrgMemberRole> roles);

    void deleteByOrganizationIdAndUserId(Long orgId, Long userId);

    List<OrganizationMember> findByUserIdAndRoleIn(Long userId, List<OrgMemberRole> roles);
}
