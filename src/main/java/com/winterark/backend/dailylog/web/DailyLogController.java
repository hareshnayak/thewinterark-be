package com.winterark.backend.dailylog.web;

import com.winterark.backend.dailylog.payload.AdHocTaskRequestDTO;
import com.winterark.backend.dailylog.payload.DailyLogResponseDTO;
import com.winterark.backend.dailylog.payload.DailyTaskResponseDTO;
import com.winterark.backend.dailylog.payload.TaskToggleRequestDTO;
import com.winterark.backend.dailylog.service.DailyLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DailyLogController {

    private final DailyLogService dailyLogService;

    @GetMapping("/goals/{goalId}/logs")
    public ResponseEntity<DailyLogResponseDTO> getLogs(
            @PathVariable UUID goalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(dailyLogService.getOrCreateDailyLog(goalId, date));
    }

    @PostMapping("/logs/{logId}/tasks/ad-hoc")
    public ResponseEntity<DailyTaskResponseDTO> addAdHocTask(
            @PathVariable UUID logId,
            @RequestBody AdHocTaskRequestDTO request) {
        return new ResponseEntity<>(dailyLogService.addAdHocTask(logId, request.getTaskContent()), HttpStatus.CREATED);
    }

    @PatchMapping("/tasks/{taskId}/toggle")
    public ResponseEntity<DailyTaskResponseDTO> toggleTaskCompletion(
            @PathVariable UUID taskId,
            @RequestBody TaskToggleRequestDTO request) {
        return ResponseEntity.ok(dailyLogService.toggleTaskCompletion(taskId, request.isCompleted()));
    }
}
