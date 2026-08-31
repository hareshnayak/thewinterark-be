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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
        Goal goal = Goal.builder()
                .user(user)
                .title(request.getTitle())
                .build();
        goal = goalRepository.save(goal);
        return GoalResponseDTO.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .createdAt(goal.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<GoalResponseDTO> getUserGoals(User user) {
        return goalRepository.findByUserId(user.getId()).stream()
                .map(g -> GoalResponseDTO.builder()
                        .id(g.getId())
                        .title(g.getTitle())
                        .createdAt(g.getCreatedAt())
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
}
