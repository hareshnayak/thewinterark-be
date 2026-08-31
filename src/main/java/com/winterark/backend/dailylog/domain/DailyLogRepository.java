package com.winterark.backend.dailylog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, UUID> {
    Optional<DailyLog> findByGoalIdAndTargetDate(UUID goalId, LocalDate targetDate);
    List<DailyLog> findByGoalIdAndTargetDateBetween(UUID goalId, LocalDate startDate, LocalDate endDate);
}
