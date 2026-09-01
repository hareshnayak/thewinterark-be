package com.winterark.backend.dailylog.service;

import com.winterark.backend.dailylog.domain.DailyLog;
import com.winterark.backend.dailylog.domain.DailyLogRepository;
import com.winterark.backend.dailylog.domain.DailyTask;
import com.winterark.backend.dailylog.domain.DailyTaskRepository;
import com.winterark.backend.dailylog.payload.DailyLogResponseDTO;
import com.winterark.backend.dailylog.payload.DailyTaskResponseDTO;
import com.winterark.backend.goal.domain.Goal;
import com.winterark.backend.goal.domain.GoalRepository;
import com.winterark.backend.goal.domain.PredefinedTask;
import com.winterark.backend.goal.domain.PredefinedTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final GoalRepository goalRepository;
    private final PredefinedTaskRepository predefinedTaskRepository;

    @Transactional
    public DailyLogResponseDTO getOrCreateDailyLog(UUID goalId, LocalDate date) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));

        DailyLog dailyLog = dailyLogRepository.findByGoalIdAndTargetDate(goalId, date)
                .orElseGet(() -> createDailyLogFromPredefined(goal, date));

        List<DailyTask> tasks = dailyTaskRepository.findByDailyLogId(dailyLog.getId());

        return DailyLogResponseDTO.builder()
                .logId(dailyLog.getId())
                .targetDate(dailyLog.getTargetDate())
                .tasks(tasks.stream().map(this::mapToTaskDTO).collect(Collectors.toList()))
                .build();
    }

    private DailyLog createDailyLogFromPredefined(Goal goal, LocalDate date) {
        DailyLog log = DailyLog.builder()
                .goal(goal)
                .targetDate(date)
                .build();
        dailyLogRepository.save(log);

        List<PredefinedTask> predefinedTasks = predefinedTaskRepository.findByGoalId(goal.getId());
        
        List<DailyTask> tasksToCreate = predefinedTasks.stream().map(pt -> 
                DailyTask.builder()
                        .dailyLog(log)
                        .taskContent(pt.getTaskContent())
                        .isCompleted(false)
                        .isAdHoc(false)
                        .build()
        ).collect(Collectors.toList());

        dailyTaskRepository.saveAll(tasksToCreate);
        return log;
    }

    @Transactional
    public DailyTaskResponseDTO addAdHocTask(UUID logId, String content) {
        DailyLog log = dailyLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found"));

        DailyTask task = DailyTask.builder()
                .dailyLog(log)
                .taskContent(content)
                .isCompleted(false)
                .isAdHoc(true)
                .build();
        
        task = dailyTaskRepository.save(task);
        return mapToTaskDTO(task);
    }

    @Transactional
    public DailyTaskResponseDTO toggleTaskCompletion(UUID taskId, boolean isCompleted) {
        DailyTask task = dailyTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        task.setCompleted(isCompleted);
        task = dailyTaskRepository.save(task);
        return mapToTaskDTO(task);
    }

    private DailyTaskResponseDTO mapToTaskDTO(DailyTask task) {
        return DailyTaskResponseDTO.builder()
                .taskId(task.getId())
                .taskContent(task.getTaskContent())
                .completed(task.isCompleted())
                .adHoc(task.isAdHoc())
                .build();
    }

}
