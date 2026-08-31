package com.winterark.backend.goal.payload;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class GoalRequestDTO {
    private String title;
    private String tagLine;
}
