package com.winterark.backend.social.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SharedGoalResponseDTO {
    private UUID goalId;
    private String title;
    private int todayProgressPercent;
    private UUID ownerId;
    private String ownerUsername;
    private int completedTasks;
    private int totalTasks;
    private int streakDays;
}
