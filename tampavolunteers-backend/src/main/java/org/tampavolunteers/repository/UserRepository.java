package org.tampavolunteers.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tampavolunteers.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderId(String providerId);

    Optional<User> findByProviderIdAndAuthProvider(String providerId, User.AuthProvider authProvider);

    boolean existsByEmail(String email);

    List<User> findByRole(User.UserRole role);

    long countByRole(User.UserRole role);

    List<User> findByLastLoginAtAfter(LocalDateTime since);

    Page<User> findByIsPublicTrue(Pageable pageable);

    List<User> findByEmailEndingWith(String suffix);
}
