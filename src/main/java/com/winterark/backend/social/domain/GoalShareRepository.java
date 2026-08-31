package com.winterark.backend.social.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoalShareRepository extends JpaRepository<GoalShare, UUID> {
    List<GoalShare> findByFriendId(UUID friendId);
}
