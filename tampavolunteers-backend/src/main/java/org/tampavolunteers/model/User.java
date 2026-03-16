package org.tampavolunteers.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User entity (volunteers, organization admins, and system admins).
 * --------------------------------------------------------------------------------------------
 * Essentially all the things that'll define our data structure for users.
 * "Models" the values that the DB should have. * Kinda. *
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_unique_provider_auth", columnList = "provider_id,auth_provider", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", length = 20)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 50)
    private UserStatus userStatus = UserStatus.VOLUNTEER;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum UserRole {
        VOLUNTEER,
        ORG_ADMIN,
        ADMIN,
        SUPER_ADMIN
    }

    public enum AuthProvider {
        LOCAL,
        GOOGLE,
        GITHUB
    }
}
