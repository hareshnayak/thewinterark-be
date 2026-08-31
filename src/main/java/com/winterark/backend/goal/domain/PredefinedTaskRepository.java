package com.winterark.backend.goal.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PredefinedTaskRepository extends JpaRepository<PredefinedTask, UUID> {
    List<PredefinedTask> findByGoalId(UUID goalId);
}
