package com.winterark.backend.social.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    Optional<Friendship> findByUserIdAndFriendId(UUID userId, UUID friendId);

    List<Friendship> findByUserIdAndStatus(UUID userId, Friendship.FriendshipStatus status);

    List<Friendship> findByFriendIdAndStatus(UUID friendId, Friendship.FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE (f.user.id = :userId OR f.friend.id = :userId) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendships(@Param("userId") UUID userId);

    @Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE ((f.user.id = :u1 AND f.friend.id = :u2) OR (f.user.id = :u2 AND f.friend.id = :u1)) AND f.status = 'ACCEPTED'")
    boolean areFriends(@Param("u1") UUID u1, @Param("u2") UUID u2);

    @Query("SELECT f FROM Friendship f WHERE (f.user.id = :u1 AND f.friend.id = :u2) OR (f.user.id = :u2 AND f.friend.id = :u1)")
    Optional<Friendship> findBetweenUsers(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
