package com.winterark.backend.social.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoalShareRepository extends JpaRepository<GoalShare, UUID> {
    List<GoalShare> findByFriendId(UUID friendId);
    List<GoalShare> findByGoalId(UUID goalId);
    Optional<GoalShare> findByGoalIdAndFriendId(UUID goalId, UUID friendId);
    void deleteByGoalIdAndFriendId(UUID goalId, UUID friendId);
    void deleteByGoalId(UUID goalId);
    boolean existsByGoalIdAndFriendId(UUID goalId, UUID friendId);
}
