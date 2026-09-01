package com.winterark.backend.goal.web;

import com.winterark.backend.auth.domain.User;
import com.winterark.backend.auth.domain.UserRepository;
import com.winterark.backend.dailylog.payload.GoalStatsResponseDTO;
import com.winterark.backend.goal.payload.GoalRequestDTO;
import com.winterark.backend.goal.payload.GoalResponseDTO;
import com.winterark.backend.goal.payload.PredefinedTaskRequestDTO;
import com.winterark.backend.goal.payload.PredefinedTaskResponseDTO;
import com.winterark.backend.goal.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<GoalResponseDTO> createGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody GoalRequestDTO request) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return new ResponseEntity<>(goalService.createGoal(user, request), HttpStatus.CREATED);
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<GoalResponseDTO> updateGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID goalId,
            @RequestBody GoalRequestDTO request) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(goalService.updateGoal(goalId, user, request));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponseDTO>> getUserGoals(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(goalService.getUserGoals(user));
    }

    @GetMapping("/{goalId}/predefined-tasks")
    public ResponseEntity<List<PredefinedTaskResponseDTO>> getPredefinedTasks(
            @PathVariable UUID goalId) {
        return ResponseEntity.ok(goalService.getPredefinedTasks(goalId));
    }

    @PostMapping("/{goalId}/predefined-tasks")
    public ResponseEntity<PredefinedTaskResponseDTO> addPredefinedTask(
            @PathVariable UUID goalId,
            @RequestBody PredefinedTaskRequestDTO request) {
        return new ResponseEntity<>(goalService.addPredefinedTask(goalId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{goalId}/predefined-tasks/{taskId}")
    public ResponseEntity<PredefinedTaskResponseDTO> updatePredefinedTask(
            @PathVariable UUID goalId,
            @PathVariable UUID taskId,
            @RequestBody PredefinedTaskRequestDTO request) {
        return ResponseEntity.ok(goalService.updatePredefinedTask(goalId, taskId, request));
    }

    @DeleteMapping("/{goalId}/predefined-tasks/{taskId}")
    public ResponseEntity<Void> deletePredefinedTask(
            @PathVariable UUID goalId,
            @PathVariable UUID taskId) {
        goalService.deletePredefinedTask(goalId, taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{goalId}/stats")
    public ResponseEntity<List<GoalStatsResponseDTO>> getGoalStats(
            @PathVariable UUID goalId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(goalService.getGoalStats(goalId, days));
    }
}
