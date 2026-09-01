package com.winterark.backend.goal.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoalStreakResponseDTO {
    private UUID goalId;
    private int currentStreak;
    private int bestStreak;
}
