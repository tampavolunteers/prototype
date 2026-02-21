package org.tampavolunteers.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.tampavolunteers.model.OrganizationMember.OrgMemberRole;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.OpportunityRepository;
import org.tampavolunteers.repository.OrganizationMemberRepository;
import org.tampavolunteers.repository.UserRepository;

import java.util.List;

@Service("authz")
@RequiredArgsConstructor
public class AuthorizationService {

    private final OrganizationMemberRepository memberRepository;
    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElse(null);
    }

    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    public boolean isOrgMember(Long orgId, String requiredRole) {
        Long userId = getCurrentUserId();
        if (userId == null) return false;
        OrgMemberRole role = OrgMemberRole.valueOf(requiredRole.toUpperCase());
        return memberRepository.existsByOrganizationIdAndUserIdAndRoleIn(orgId, userId, List.of(role));
    }

    public boolean isOrgOwnerOrAdmin(Long orgId) {
        Long userId = getCurrentUserId();
        if (userId == null) return false;
        return memberRepository.existsByOrganizationIdAndUserIdAndRoleIn(
                orgId, userId, List.of(OrgMemberRole.OWNER, OrgMemberRole.ADMIN));
    }

    public boolean canManageOpportunity(Long opportunityId) {
        Long userId = getCurrentUserId();
        if (userId == null) return false;
        return opportunityRepository.findById(opportunityId)
                .map(opp -> memberRepository.existsByOrganizationIdAndUserIdAndRoleIn(
                        opp.getOrganization().getId(), userId,
                        List.of(OrgMemberRole.OWNER, OrgMemberRole.ADMIN, OrgMemberRole.MEMBER)))
                .orElse(false);
    }
}
