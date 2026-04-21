package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u.id FROM User u JOIN u.role r WHERE UPPER(r.name) LIKE CONCAT('%', UPPER(:roleKeyword), '%')")
    List<Long> findUserIdsByRoleKeyword(@Param("roleKeyword") String roleKeyword);

    // TODO: Add more custom query methods as needed
}
