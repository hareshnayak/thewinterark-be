package com.winterark.backend.dailylog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DailyTaskRepository extends JpaRepository<DailyTask, UUID> {
    List<DailyTask> findByDailyLogId(UUID dailyLogId);
}
