package org.tampavolunteers.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.tampavolunteers.dto.OrganizationAdminDTO;
import org.tampavolunteers.dto.UserSummaryDTO;
import org.tampavolunteers.exception.NotFoundException;
import org.tampavolunteers.model.Organization;
import org.tampavolunteers.model.User;
import org.tampavolunteers.model.UserStatus;
import org.tampavolunteers.repository.UserRepository;
import org.tampavolunteers.service.OrganizationService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final OrganizationService organizationService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserSummaryDTO>> listUsers(Pageable pageable) {
        Page<UserSummaryDTO> users = userRepository.findAll(pageable).map(this::toSummary);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserSummaryDTO> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return ResponseEntity.ok(toSummary(user));
    }

    @GetMapping("/users/signins")
    public ResponseEntity<List<UserSummaryDTO>> getRecentSignins() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<UserSummaryDTO> users = userRepository.findByLastLoginAtAfter(since)
                .stream().map(this::toSummary).toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/organizations")
    public ResponseEntity<Page<OrganizationAdminDTO>> listAllOrganizations(Pageable pageable) {
        return ResponseEntity.ok(organizationService.getAllOrganizationsAdmin(pageable));
    }

    @PutMapping("/organizations/{id}/verify")
    public ResponseEntity<Organization> verifyOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.verifyOrganization(id));
    }

    private UserSummaryDTO toSummary(User user) {
        return new UserSummaryDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getUserStatus() != null ? user.getUserStatus() : UserStatus.VOLUNTEER,
                user.getIsPublic(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
