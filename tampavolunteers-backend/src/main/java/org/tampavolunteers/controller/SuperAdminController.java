package org.tampavolunteers.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.tampavolunteers.dto.AuditLogDTO;
import org.tampavolunteers.dto.ChangeRoleDTO;
import org.tampavolunteers.exception.BadRequestException;
import org.tampavolunteers.exception.NotFoundException;
import org.tampavolunteers.model.AdminAuditLog;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.AdminAuditLogRepository;
import org.tampavolunteers.repository.UserRepository;
import org.tampavolunteers.service.AuditService;

@RestController
@RequestMapping("/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class SuperAdminController {

    private final UserRepository userRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final AuditService auditService;

    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> changeUserRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleDTO dto,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        User admin = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        User target = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (target.getId().equals(admin.getId())) {
            throw new BadRequestException("Cannot change your own role");
        }

        // Prevent removing the last SUPER_ADMIN
        if (target.getRole() == User.UserRole.SUPER_ADMIN
                && dto.getRole() != User.UserRole.SUPER_ADMIN) {
            long superAdminCount = userRepository.countByRole(User.UserRole.SUPER_ADMIN);
            if (superAdminCount <= 1) {
                throw new BadRequestException("Cannot demote the last SUPER_ADMIN");
            }
        }

        User.UserRole oldRole = target.getRole();
        target.setRole(dto.getRole());
        userRepository.save(target);

        auditService.logAction(
                admin.getId(),
                "CHANGE_ROLE",
                "USER",
                target.getId(),
                String.format("{\"from\":\"%s\",\"to\":\"%s\"}", oldRole, dto.getRole()),
                request.getRemoteAddr()
        );

        return ResponseEntity.ok(target);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        User admin = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        User target = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (target.getId().equals(admin.getId())) {
            throw new BadRequestException("Cannot delete your own account via this endpoint");
        }

        if (target.getRole() == User.UserRole.SUPER_ADMIN) {
            long superAdminCount = userRepository.countByRole(User.UserRole.SUPER_ADMIN);
            if (superAdminCount <= 1) {
                throw new BadRequestException("Cannot delete the last SUPER_ADMIN");
            }
        }

        auditService.logAction(
                admin.getId(),
                "DELETE_USER",
                "USER",
                target.getId(),
                String.format("{\"email\":\"%s\",\"role\":\"%s\"}", target.getEmail(), target.getRole()),
                request.getRemoteAddr()
        );

        userRepository.delete(target);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-log")
    public ResponseEntity<Page<AuditLogDTO>> getAuditLog(Pageable pageable) {
        Page<AuditLogDTO> logs = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDTO);
        return ResponseEntity.ok(logs);
    }

    private AuditLogDTO toDTO(AdminAuditLog log) {
        return new AuditLogDTO(
                log.getId(),
                log.getAdminUser().getId(),
                log.getAdminUser().getEmail(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }
}
