package org.tampavolunteers.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tampavolunteers.dto.PublicProfileDTO;
import org.tampavolunteers.dto.UpdateProfileDTO;
import org.tampavolunteers.dto.UpdateStatusDTO;
import org.tampavolunteers.dto.UserProfileDTO;
import org.tampavolunteers.dto.VisibilityDTO;
import org.tampavolunteers.exception.BadRequestException;
import org.tampavolunteers.exception.NotFoundException;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileDTO getUserProfile(String email) {
        User user = findByEmail(email);
        return toProfileDTO(user);
    }

    @Transactional
    public UserProfileDTO updateProfile(String email, UpdateProfileDTO dto) {
        User user = findByEmail(email);

        if (dto.getFirstName() != null && !dto.getFirstName().isBlank()) {
            user.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null && !dto.getLastName().isBlank()) {
            user.setLastName(dto.getLastName());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }

        return toProfileDTO(userRepository.save(user));
    }

    @Transactional
    public UserProfileDTO updateStatus(String email, UpdateStatusDTO dto) {
        User user = findByEmail(email);
        if (dto.getUserStatus() == null) {
            throw new BadRequestException("userStatus is required");
        }
        user.setUserStatus(dto.getUserStatus());
        return toProfileDTO(userRepository.save(user));
    }

    @Transactional
    public UserProfileDTO updateVisibility(String email, VisibilityDTO dto) {
        User user = findByEmail(email);
        if (dto.getIsPublic() == null) {
            throw new BadRequestException("isPublic is required");
        }
        user.setIsPublic(dto.getIsPublic());
        return toProfileDTO(userRepository.save(user));
    }

    public Page<PublicProfileDTO> getPublicProfiles(Pageable pageable) {
        return userRepository.findByIsPublicTrue(pageable).map(this::toPublicDTO);
    }

    public PublicProfileDTO getPublicProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!Boolean.TRUE.equals(user.getIsPublic())) {
            throw new NotFoundException("User not found");
        }
        return toPublicDTO(user);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private UserProfileDTO toProfileDTO(User user) {
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getIsPublic(),
                user.getUserStatus(),
                user.getRole(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }

    private PublicProfileDTO toPublicDTO(User user) {
        return new PublicProfileDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getUserStatus(),
                user.getCreatedAt()
        );
    }
}
