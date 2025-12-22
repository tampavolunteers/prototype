package org.tampavolunteers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tampavolunteers.model.User;

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
}
