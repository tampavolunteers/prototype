package org.tampavolunteers.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.tampavolunteers.dto.CreateOrganizationDTO;
import org.tampavolunteers.dto.InviteMemberDTO;
import org.tampavolunteers.dto.MemberRoleDTO;
import org.tampavolunteers.dto.MyOrgDTO;
import org.tampavolunteers.dto.OrgMemberDTO;
import org.tampavolunteers.exception.NotFoundException;
import org.tampavolunteers.model.Organization;
import org.tampavolunteers.model.OrganizationMember;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.UserRepository;
import org.tampavolunteers.service.OrganizationService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Organization>> getOrganizations() {
        return ResponseEntity.ok(organizationService.getOrganizations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getOrganization(id));
    }

    @PostMapping
    public ResponseEntity<Organization> createOrganization(
            @Valid @RequestBody CreateOrganizationDTO dto,
            @AuthenticationPrincipal UserDetails principal) {
        User currentUser = getCurrentUser(principal);
        Organization created = organizationService.createOrganization(dto, currentUser);
        return ResponseEntity.created(URI.create("/organizations/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Organization> updateOrganization(
            @PathVariable Long id,
            @RequestBody CreateOrganizationDTO dto,
            @AuthenticationPrincipal UserDetails principal) {
        User currentUser = getCurrentUser(principal);
        return ResponseEntity.ok(organizationService.updateOrganization(id, dto, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        User currentUser = getCurrentUser(principal);
        organizationService.deleteOrganization(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<MyOrgDTO>> getMyOrganizations(
            @AuthenticationPrincipal UserDetails principal) {
        User currentUser = getCurrentUser(principal);
        return ResponseEntity.ok(organizationService.getMyOrganizations(currentUser));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<OrgMemberDTO>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getMembers(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<OrganizationMember> addMember(
            @PathVariable Long id,
            @Valid @RequestBody InviteMemberDTO dto,
            @AuthenticationPrincipal UserDetails principal) {
        User currentUser = getCurrentUser(principal);
        return ResponseEntity.ok(organizationService.addMember(id, dto, currentUser));
    }

    @PutMapping("/{id}/members/{userId}/role")
    public ResponseEntity<OrganizationMember> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody MemberRoleDTO dto,
            @AuthenticationPrincipal UserDetails principal) {
        User currentUser = getCurrentUser(principal);
        return ResponseEntity.ok(organizationService.updateMemberRole(id, userId, dto, currentUser));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails principal) {
        User currentUser = getCurrentUser(principal);
        organizationService.removeMember(id, userId, currentUser);
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
