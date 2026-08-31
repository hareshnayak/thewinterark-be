package com.winterark.backend.social.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    Optional<Friendship> findByUserIdAndFriendId(UUID userId, UUID friendId);
    List<Friendship> findByUserIdAndStatus(UUID userId, Friendship.FriendshipStatus status);
}
