package com.winterark.backend.dailylog.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TaskToggleRequestDTO {
    @JsonProperty("isCompleted")
    private boolean completed;
}
