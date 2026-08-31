package com.winterark.backend.dailylog.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyLogResponseDTO {
    private UUID logId;
    private LocalDate targetDate;
    private List<DailyTaskResponseDTO> tasks;
}
