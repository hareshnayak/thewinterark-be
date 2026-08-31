package com.winterark.backend.dailylog.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoalStatsResponseDTO {
    private LocalDate date;
    private int completionPercent;
}
