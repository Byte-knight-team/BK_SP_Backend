package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Customer;
import com.ByteKnights.com.resturarent_system.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserId(Long userId);

    Optional<Customer> findByUser(User user);

    Optional<Customer> findByUserPhone(String userIdentifier);

    Optional<Customer> findByUserEmail(String userIdentifier);

    @Query("SELECT c FROM Customer c JOIN FETCH c.user u WHERE u.email = :identifier OR u.phone = :identifier")
    Optional<Customer> findByUserEmailOrUserPhone(String identifier);

    /*
     * Used by SUPER_ADMIN customer management.
     * Loads Customer together with connected User and Role to avoid lazy-loading problems.
     */
    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("SELECT c FROM Customer c ORDER BY c.id DESC")
    List<Customer> findAllWithUserOrderByIdDesc();

    /*
     * Used by SUPER_ADMIN customer detail, activate, and deactivate operations.
     */
    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("SELECT c FROM Customer c WHERE c.id = :id")
    Optional<Customer> findByIdWithUser(Long id);
}