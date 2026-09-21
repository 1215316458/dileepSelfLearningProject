package com.ecommerce.user_service.repository;

import com.ecommerce.user_service.domain.entity.User;
import com.ecommerce.user_service.domain.enums.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // derived query — Spring generates: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // @EntityGraph — solves the N+1 problem for addresses
    // Without this: loading 10 users fires 10 extra SELECT queries for addresses (N+1)
    // With this:    one JOIN query fetches users + addresses together
    @EntityGraph(attributePaths = "addresses")
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithAddresses(@Param("id") Long id);

    // @ElementCollection (roles) is EAGER so no N+1 issue there
    // but addresses are LAZY — @EntityGraph forces the JOIN only when we need it
    @EntityGraph(attributePaths = "addresses")
    List<User> findAll();

    // find users by role — uses @ElementCollection join table
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") Role role);
}
