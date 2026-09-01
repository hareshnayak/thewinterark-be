package com.winterark.backend.goal.service;

import com.winterark.backend.auth.domain.User;
import com.winterark.backend.dailylog.domain.DailyLog;
import com.winterark.backend.dailylog.domain.DailyLogRepository;
import com.winterark.backend.dailylog.domain.DailyTask;
import com.winterark.backend.dailylog.domain.DailyTaskRepository;
import com.winterark.backend.dailylog.domain.TaskStatus;
import com.winterark.backend.dailylog.payload.GoalStatsResponseDTO;
import com.winterark.backend.goal.domain.Goal;
import com.winterark.backend.goal.domain.GoalRepository;
import com.winterark.backend.goal.domain.PredefinedTask;
import com.winterark.backend.goal.domain.PredefinedTaskRepository;
import com.winterark.backend.goal.payload.GoalRequestDTO;
import com.winterark.backend.goal.payload.GoalResponseDTO;
import com.winterark.backend.goal.payload.GoalStreakResponseDTO;
import com.winterark.backend.goal.payload.PredefinedTaskRequestDTO;
import com.winterark.backend.goal.payload.PredefinedTaskResponseDTO;
import com.winterark.backend.social.domain.GoalShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final PredefinedTaskRepository predefinedTaskRepository;
    private final DailyLogRepository dailyLogRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final GoalShareRepository goalShareRepository;

    @Transactional
    public GoalResponseDTO createGoal(User user, GoalRequestDTO request) {
        Set<DayOfWeek> activeDays = (request.getActiveDays() != null && !request.getActiveDays().isEmpty())
                ? request.getActiveDays()
                : EnumSet.allOf(DayOfWeek.class);

        Goal goal = Goal.builder()
                .user(user)
                .title(request.getTitle())
                .tagLine(request.getTagLine())
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                .endDate(request.getEndDate())
                .activeDays(activeDays)
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .archived(false)
                .build();
        goal = goalRepository.save(goal);
        return mapToGoalResponse(goal);
    }

    @Transactional
    public GoalResponseDTO updateGoal(UUID goalId, User user, GoalRequestDTO request) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not your goal to edit");
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            goal.setTitle(request.getTitle().trim());
        }
        if (request.getTagLine() != null) {
            goal.setTagLine(request.getTagLine().trim());
        }

        // Start Date Lock Rule: Once tasks are checked off, start date cannot be changed
        if (request.getStartDate() != null && !request.getStartDate().equals(goal.getStartDate())) {
            boolean hasCompleted = dailyTaskRepository.existsByDailyLogGoalIdAndStatus(goalId, TaskStatus.COMPLETED);
            if (hasCompleted) {
                throw new IllegalArgumentException("Start date cannot be changed once tasks have already been checked off.");
            }
            goal.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            goal.setEndDate(request.getEndDate());
        }
        if (request.getActiveDays() != null && !request.getActiveDays().isEmpty()) {
            goal.setActiveDays(request.getActiveDays());
        }
        if (request.getTimezone() != null && !request.getTimezone().trim().isEmpty()) {
            goal.setTimezone(request.getTimezone().trim());
        }

        goal = goalRepository.save(goal);
        return mapToGoalResponse(goal);
    }

    @Transactional
    public GoalResponseDTO archiveGoal(UUID goalId, User user) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not your goal to archive");
        }
        goal.setArchived(true);
        goal = goalRepository.save(goal);
        return mapToGoalResponse(goal);
    }

    @Transactional
    public GoalResponseDTO unarchiveGoal(UUID goalId, User user) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not your goal to unarchive");
        }
        goal.setArchived(false);
        goal = goalRepository.save(goal);
        return mapToGoalResponse(goal);
    }

    @Transactional
    public void deleteGoal(UUID goalId, User user) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not your goal to delete");
        }

        // 1. Delete all daily tasks under this goal's daily logs
        List<DailyLog> logs = dailyLogRepository.findByGoalId(goalId);
        for (DailyLog log : logs) {
            List<DailyTask> tasks = dailyTaskRepository.findByDailyLogId(log.getId());
            dailyTaskRepository.deleteAll(tasks);
        }

        // 2. Delete daily logs
        dailyLogRepository.deleteByGoalId(goalId);

        // 3. Delete predefined tasks
        predefinedTaskRepository.deleteByGoalId(goalId);

        // 4. Delete goal shares
        goalShareRepository.deleteByGoalId(goalId);

        // 5. Delete the goal entity
        goalRepository.delete(goal);
    }

    @Transactional(readOnly = true)
    public List<GoalResponseDTO> getUserGoals(User user) {
        return goalRepository.findByUserIdAndArchivedFalse(user.getId()).stream()
                .map(this::mapToGoalResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GoalResponseDTO> getArchivedGoals(User user) {
        return goalRepository.findByUserId(user.getId()).stream()
                .filter(Goal::isArchived)
                .map(this::mapToGoalResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PredefinedTaskResponseDTO> getPredefinedTasks(UUID goalId) {
        return predefinedTaskRepository.findByGoalId(goalId).stream()
                .map(t -> PredefinedTaskResponseDTO.builder()
                        .id(t.getId())
                        .taskContent(t.getTaskContent())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public PredefinedTaskResponseDTO addPredefinedTask(UUID goalId, PredefinedTaskRequestDTO request) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        PredefinedTask task = PredefinedTask.builder()
                .goal(goal)
                .taskContent(request.getTaskContent())
                .build();
        task = predefinedTaskRepository.save(task);
        return PredefinedTaskResponseDTO.builder()
                .id(task.getId())
                .taskContent(task.getTaskContent())
                .build();
    }

    @Transactional
    public PredefinedTaskResponseDTO updatePredefinedTask(UUID goalId, UUID taskId, PredefinedTaskRequestDTO request) {
        PredefinedTask task = predefinedTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Predefined task not found"));
        if (!task.getGoal().getId().equals(goalId)) {
            throw new IllegalArgumentException("Task does not belong to this goal");
        }
        task.setTaskContent(request.getTaskContent());
        task = predefinedTaskRepository.save(task);
        return PredefinedTaskResponseDTO.builder()
                .id(task.getId())
                .taskContent(task.getTaskContent())
                .build();
    }

    @Transactional
    public void deletePredefinedTask(UUID goalId, UUID taskId) {
        PredefinedTask task = predefinedTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Predefined task not found"));
        if (!task.getGoal().getId().equals(goalId)) {
            throw new IllegalArgumentException("Task does not belong to this goal");
        }
        predefinedTaskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<GoalStatsResponseDTO> getGoalStats(UUID goalId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<DailyLog> logs = dailyLogRepository.findByGoalIdAndTargetDateBetween(goalId, startDate, endDate);
        List<GoalStatsResponseDTO> stats = new ArrayList<>();

        for (DailyLog log : logs) {
            List<DailyTask> tasks = dailyTaskRepository.findByDailyLogId(log.getId());
            if (tasks.isEmpty()) continue;

            long completedCount = tasks.stream().filter(DailyTask::isCompleted).count();
            int percent = (int) ((completedCount * 100.0f) / tasks.size());
            stats.add(GoalStatsResponseDTO.builder()
                    .date(log.getTargetDate())
                    .completionPercent(percent)
                    .build());
        }

        return stats;
    }

    @Transactional(readOnly = true)
    public GoalStreakResponseDTO getGoalStreak(UUID goalId) {
        LocalDate today = LocalDate.now();
        List<DailyLog> pastLogs = dailyLogRepository.findByGoalIdAndTargetDateLessThanEqualOrderByTargetDateDesc(goalId, today);

        int currentStreak = 0;
        int bestStreak = 0;
        int tempStreak = 0;
        boolean calculatingCurrent = true;

        for (DailyLog logItem : pastLogs) {
            List<DailyTask> tasks = dailyTaskRepository.findByDailyLogId(logItem.getId());
            if (tasks.isEmpty()) continue;

            boolean allDone = tasks.stream().allMatch(DailyTask::isCompleted);
            if (allDone) {
                tempStreak++;
                if (calculatingCurrent) {
                    currentStreak++;
                }
                if (tempStreak > bestStreak) {
                    bestStreak = tempStreak;
                }
            } else {
                if (logItem.getTargetDate().equals(today) && calculatingCurrent) {
                    // Today is still ongoing
                    continue;
                }
                calculatingCurrent = false;
                tempStreak = 0;
            }
        }

        if (bestStreak < currentStreak) {
            bestStreak = currentStreak;
        }

        return GoalStreakResponseDTO.builder()
                .goalId(goalId)
                .currentStreak(currentStreak)
                .bestStreak(bestStreak)
                .build();
    }

    private GoalResponseDTO mapToGoalResponse(Goal goal) {
        int streak = getGoalStreak(goal.getId()).getCurrentStreak();
        boolean hasCompleted = dailyTaskRepository.existsByDailyLogGoalIdAndStatus(goal.getId(), TaskStatus.COMPLETED);

        return GoalResponseDTO.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .tagLine(goal.getTagLine())
                .startDate(goal.getStartDate())
                .endDate(goal.getEndDate())
                .activeDays(goal.getActiveDays())
                .timezone(goal.getTimezone())
                .currentStreak(streak)
                .archived(goal.isArchived())
                .hasCompletedTasks(hasCompleted)
                .createdAt(goal.getCreatedAt())
                .build();
    }
}
