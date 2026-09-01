package com.winterark.backend.goal.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoalResponseDTO {
    private UUID id;
    private String title;
    private String tagLine;
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<DayOfWeek> activeDays;
    private String timezone;
    private LocalDateTime createdAt;
}
