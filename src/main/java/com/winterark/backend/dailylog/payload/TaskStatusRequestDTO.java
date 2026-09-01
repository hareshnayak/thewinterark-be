package com.winterark.backend.dailylog.payload;

import com.winterark.backend.dailylog.domain.TaskStatus;
import lombok.Data;

@Data
public class TaskStatusRequestDTO {
    private TaskStatus status;
}
