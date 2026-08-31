package com.winterark.backend.dailylog.payload;

import lombok.Data;

@Data
public class TaskToggleRequestDTO {
    private boolean isCompleted;
}
