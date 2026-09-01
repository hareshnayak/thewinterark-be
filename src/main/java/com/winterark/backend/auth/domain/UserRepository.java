package com.winterark.backend.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND u.id != :currentUserId " +
           "AND u.id NOT IN (SELECT f.friend.id FROM Friendship f WHERE f.user.id = :currentUserId) " +
           "AND u.id NOT IN (SELECT f.user.id FROM Friendship f WHERE f.friend.id = :currentUserId)")
    List<User> searchUsersNotConnected(@Param("query") String query, @Param("currentUserId") UUID currentUserId);
}
