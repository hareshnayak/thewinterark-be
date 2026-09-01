package com.winterark.backend.dailylog.scheduler;

import com.winterark.backend.dailylog.domain.DailyLog;
import com.winterark.backend.dailylog.domain.DailyLogRepository;
import com.winterark.backend.dailylog.domain.DailyTask;
import com.winterark.backend.dailylog.domain.DailyTaskRepository;
import com.winterark.backend.dailylog.domain.TaskStatus;
import com.winterark.backend.goal.domain.Goal;
import com.winterark.backend.goal.domain.GoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutoSkipScheduler {

    private final GoalRepository goalRepository;
    private final DailyLogRepository dailyLogRepository;
    private final DailyTaskRepository dailyTaskRepository;

    /**
     * Runs periodically at the top of every hour to auto-skip pending tasks for any goal
     * whose local midnight has passed in its configured timezone.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void autoSkipPendingTasks() {
        log.info("Running timezone-aware auto-skip job for pending tasks...");

        List<Goal> goals = goalRepository.findAll();
        int totalSkipped = 0;

        for (Goal goal : goals) {
            ZoneId zoneId;
            try {
                zoneId = (goal.getTimezone() != null && !goal.getTimezone().trim().isEmpty())
                        ? ZoneId.of(goal.getTimezone().trim())
                        : ZoneId.of("UTC");
            } catch (Exception e) {
                zoneId = ZoneId.of("UTC");
            }

            LocalDate localToday = LocalDate.now(zoneId);

            // Find all daily logs for this goal with targetDate strictly before local today
            List<DailyLog> pastLogs = dailyLogRepository.findByGoalIdAndTargetDateBetween(
                    goal.getId(),
                    localToday.minusYears(1),
                    localToday.minusDays(1)
            );

            for (DailyLog logItem : pastLogs) {
                List<DailyTask> pendingTasks = dailyTaskRepository.findPendingTasksByDailyLogId(logItem.getId());
                for (DailyTask task : pendingTasks) {
                    task.setStatus(TaskStatus.SKIPPED);
                    dailyTaskRepository.save(task);
                    totalSkipped++;
                }
            }
        }

        log.info("Timezone-aware auto-skip job completed. Total tasks marked as SKIPPED: {}", totalSkipped);
    }
}
