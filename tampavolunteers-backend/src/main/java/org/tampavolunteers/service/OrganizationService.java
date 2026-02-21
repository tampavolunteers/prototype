package org.tampavolunteers.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tampavolunteers.dto.CreateOrganizationDTO;
import org.tampavolunteers.dto.InviteMemberDTO;
import org.tampavolunteers.dto.MemberRoleDTO;
import org.tampavolunteers.dto.OrganizationAdminDTO;
import org.tampavolunteers.exception.BadRequestException;
import org.tampavolunteers.exception.NotFoundException;
import org.tampavolunteers.model.Organization;
import org.tampavolunteers.model.OrganizationMember;
import org.tampavolunteers.model.OrganizationMember.OrgMemberRole;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.OrganizationMemberRepository;
import org.tampavolunteers.repository.OrganizationRepository;
import org.tampavolunteers.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public List<Organization> getOrganizations() {
        return organizationRepository.findByVerified(true);
    }

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<OrganizationAdminDTO> getAllOrganizationsAdmin(Pageable pageable) {
        return organizationRepository.findAll(pageable).map(this::toAdminDTO);
    }

    private OrganizationAdminDTO toAdminDTO(Organization org) {
        return new OrganizationAdminDTO(
                org.getId(),
                org.getName(),
                org.getDescription(),
                org.getWebsite(),
                org.getContactEmail(),
                org.getContactPhone(),
                org.getCity(),
                org.getState(),
                org.getVerified(),
                org.getSeeded(),
                org.getUser() != null ? org.getUser().getEmail() : null,
                org.getCreatedAt()
        );
    }

    public Organization getOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    @Transactional
    public Organization createOrganization(CreateOrganizationDTO dto, User currentUser) {
        Organization org = new Organization();
        org.setName(dto.getName());
        org.setDescription(dto.getDescription());
        org.setWebsite(dto.getWebsite());
        org.setContactEmail(dto.getContactEmail());
        org.setContactPhone(dto.getContactPhone());
        org.setStreet(dto.getStreet());
        org.setCity(dto.getCity());
        org.setState(dto.getState());
        org.setZip(dto.getZip());
        org.setVerified(false);
        org.setUser(currentUser);

        Organization saved = organizationRepository.save(org);

        // Create OWNER membership entry
        OrganizationMember ownerMembership = new OrganizationMember();
        ownerMembership.setOrganization(saved);
        ownerMembership.setUser(currentUser);
        ownerMembership.setRole(OrgMemberRole.OWNER);
        memberRepository.save(ownerMembership);

        return saved;
    }

    @Transactional
    public Organization updateOrganization(Long id, CreateOrganizationDTO dto, User currentUser) {
        Organization org = getOrganization(id);
        requireOwnerOrAdminOrSuperAdmin(org, currentUser);

        if (dto.getName() != null && !dto.getName().isBlank()) {
            org.setName(dto.getName());
        }
        if (dto.getDescription() != null) org.setDescription(dto.getDescription());
        if (dto.getWebsite() != null) org.setWebsite(dto.getWebsite());
        if (dto.getContactEmail() != null) org.setContactEmail(dto.getContactEmail());
        if (dto.getContactPhone() != null) org.setContactPhone(dto.getContactPhone());
        if (dto.getStreet() != null) org.setStreet(dto.getStreet());
        if (dto.getCity() != null) org.setCity(dto.getCity());
        if (dto.getState() != null) org.setState(dto.getState());
        if (dto.getZip() != null) org.setZip(dto.getZip());

        return organizationRepository.save(org);
    }

    @Transactional
    public void deleteOrganization(Long id, User currentUser) {
        Organization org = getOrganization(id);
        requireOwnerOrSuperAdmin(org, currentUser);
        organizationRepository.delete(org);
    }

    public List<OrganizationMember> getMembers(Long orgId) {
        getOrganization(orgId); // ensure org exists
        return memberRepository.findByOrganizationId(orgId);
    }

    @Transactional
    public OrganizationMember addMember(Long orgId, InviteMemberDTO dto, User currentUser) {
        Organization org = getOrganization(orgId);
        requireOwnerOrAdmin(org, currentUser);

        if (memberRepository.findByOrganizationIdAndUserId(orgId, dto.getUserId()).isPresent()) {
            throw new BadRequestException("User is already a member of this organization");
        }

        User invitee = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        OrganizationMember member = new OrganizationMember();
        member.setOrganization(org);
        member.setUser(invitee);
        member.setRole(dto.getRole() != null ? dto.getRole() : OrgMemberRole.MEMBER);
        member.setInvitedBy(currentUser);

        return memberRepository.save(member);
    }

    @Transactional
    public OrganizationMember updateMemberRole(Long orgId, Long userId, MemberRoleDTO dto, User currentUser) {
        getOrganization(orgId); // ensure org exists
        requireOwner(orgId, currentUser);

        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        // Cannot change own role
        if (member.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Cannot change your own role");
        }

        member.setRole(dto.getRole());
        return memberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long orgId, Long userId, User currentUser) {
        getOrganization(orgId); // ensure org exists
        requireOwnerOrAdmin(orgId, currentUser);

        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        // Cannot remove the owner
        if (member.getRole() == OrgMemberRole.OWNER) {
            throw new BadRequestException("Cannot remove the organization owner");
        }

        memberRepository.delete(member);
    }

    @Transactional
    public Organization verifyOrganization(Long id) {
        Organization org = getOrganization(id);
        org.setVerified(true);
        return organizationRepository.save(org);
    }

    // --- Private helpers ---

    private void requireOwnerOrAdminOrSuperAdmin(Organization org, User user) {
        if (isSuperAdmin(user)) return;
        if (isOrgOwnerOrAdmin(org.getId(), user.getId())) return;
        throw new AccessDeniedException("Not authorized to manage this organization");
    }

    private void requireOwnerOrSuperAdmin(Organization org, User user) {
        if (isSuperAdmin(user)) return;
        if (isOrgOwner(org.getId(), user.getId())) return;
        throw new AccessDeniedException("Not authorized to delete this organization");
    }

    private void requireOwnerOrAdmin(Organization org, User user) {
        if (isSuperAdmin(user)) return;
        if (isOrgOwnerOrAdmin(org.getId(), user.getId())) return;
        throw new AccessDeniedException("Not authorized to manage members");
    }

    private void requireOwnerOrAdmin(Long orgId, User user) {
        if (isSuperAdmin(user)) return;
        if (isOrgOwnerOrAdmin(orgId, user.getId())) return;
        throw new AccessDeniedException("Not authorized to manage members");
    }

    private void requireOwner(Long orgId, User user) {
        if (isSuperAdmin(user)) return;
        if (isOrgOwner(orgId, user.getId())) return;
        throw new AccessDeniedException("Only the organization owner can change member roles");
    }

    private boolean isSuperAdmin(User user) {
        return user.getRole() == User.UserRole.SUPER_ADMIN;
    }

    private boolean isOrgOwnerOrAdmin(Long orgId, Long userId) {
        return memberRepository.existsByOrganizationIdAndUserIdAndRoleIn(
                orgId, userId, List.of(OrgMemberRole.OWNER, OrgMemberRole.ADMIN));
    }

    private boolean isOrgOwner(Long orgId, Long userId) {
        return memberRepository.existsByOrganizationIdAndUserIdAndRoleIn(
                orgId, userId, List.of(OrgMemberRole.OWNER));
    }
}
