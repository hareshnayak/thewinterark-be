package com.winterark.backend.dailylog.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyTaskResponseDTO {
    private UUID taskId;
    private String taskContent;
    private boolean isCompleted;
    private boolean isAdHoc;
}
