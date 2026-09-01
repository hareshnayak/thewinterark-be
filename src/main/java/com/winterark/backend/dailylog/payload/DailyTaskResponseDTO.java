package com.winterark.backend.dailylog.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.winterark.backend.dailylog.domain.TaskStatus;
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
    private TaskStatus status;
    @JsonProperty("isCompleted")
    private boolean completed;
    @JsonProperty("isAdHoc")
    private boolean adHoc;
}
