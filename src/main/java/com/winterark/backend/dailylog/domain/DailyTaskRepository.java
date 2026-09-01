package com.winterark.backend.dailylog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DailyTaskRepository extends JpaRepository<DailyTask, UUID> {
    List<DailyTask> findByDailyLogId(UUID dailyLogId);
    List<DailyTask> findByDailyLogIdAndStatus(UUID dailyLogId, TaskStatus status);

    @Query("SELECT t FROM DailyTask t WHERE t.dailyLog.goal.id = :goalId AND t.status = 'SKIPPED' ORDER BY t.dailyLog.targetDate DESC")
    List<DailyTask> findSkippedTasksByGoalId(@Param("goalId") UUID goalId);

    @Query("SELECT t FROM DailyTask t WHERE t.dailyLog.id = :dailyLogId AND t.status = 'PENDING'")
    List<DailyTask> findPendingTasksByDailyLogId(@Param("dailyLogId") UUID dailyLogId);
}
