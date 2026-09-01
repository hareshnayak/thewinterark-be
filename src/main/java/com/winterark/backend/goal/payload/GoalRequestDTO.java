package com.winterark.backend.goal.payload;

import lombok.Data;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Data
public class GoalRequestDTO {
    private String title;
    private String tagLine;
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<DayOfWeek> activeDays;
    private String timezone;
}
