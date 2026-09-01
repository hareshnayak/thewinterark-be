package com.winterark.backend.goal.service;

import com.winterark.backend.auth.domain.User;
import com.winterark.backend.dailylog.domain.DailyLog;
import com.winterark.backend.dailylog.domain.DailyLogRepository;
import com.winterark.backend.dailylog.domain.DailyTask;
import com.winterark.backend.dailylog.domain.DailyTaskRepository;
import com.winterark.backend.dailylog.payload.GoalStatsResponseDTO;
import com.winterark.backend.goal.domain.Goal;
import com.winterark.backend.goal.domain.GoalRepository;
import com.winterark.backend.goal.domain.PredefinedTask;
import com.winterark.backend.goal.domain.PredefinedTaskRepository;
import com.winterark.backend.goal.payload.GoalRequestDTO;
import com.winterark.backend.goal.payload.GoalResponseDTO;
import com.winterark.backend.goal.payload.PredefinedTaskRequestDTO;
import com.winterark.backend.goal.payload.PredefinedTaskResponseDTO;
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
        if (request.getStartDate() != null) {
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

    @Transactional(readOnly = true)
    public List<GoalResponseDTO> getUserGoals(User user) {
        return goalRepository.findByUserId(user.getId()).stream()
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

    private GoalResponseDTO mapToGoalResponse(Goal goal) {
        return GoalResponseDTO.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .tagLine(goal.getTagLine())
                .startDate(goal.getStartDate())
                .endDate(goal.getEndDate())
                .activeDays(goal.getActiveDays())
                .timezone(goal.getTimezone())
                .createdAt(goal.getCreatedAt())
                .build();
    }
}
