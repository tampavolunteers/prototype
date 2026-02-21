package org.tampavolunteers.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.tampavolunteers.dto.PublicProfileDTO;
import org.tampavolunteers.dto.UpdateProfileDTO;
import org.tampavolunteers.dto.UpdateStatusDTO;
import org.tampavolunteers.dto.UserProfileDTO;
import org.tampavolunteers.dto.VisibilityDTO;
import org.tampavolunteers.service.UserService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getProfile(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.getUserProfile(principal.getUsername()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody UpdateProfileDTO dto) {
        return ResponseEntity.ok(userService.updateProfile(principal.getUsername(), dto));
    }

    @PutMapping("/me/status")
    public ResponseEntity<UserProfileDTO> updateStatus(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody UpdateStatusDTO dto) {
        return ResponseEntity.ok(userService.updateStatus(principal.getUsername(), dto));
    }

    @PutMapping("/me/visibility")
    public ResponseEntity<UserProfileDTO> updateVisibility(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody VisibilityDTO dto) {
        return ResponseEntity.ok(userService.updateVisibility(principal.getUsername(), dto));
    }

    @GetMapping("/public")
    public ResponseEntity<Page<PublicProfileDTO>> getPublicProfiles(Pageable pageable) {
        return ResponseEntity.ok(userService.getPublicProfiles(pageable));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<PublicProfileDTO> getPublicProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getPublicProfile(id));
    }
}
