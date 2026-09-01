package com.winterark.backend.dailylog.domain;

import com.winterark.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

@Entity
@Table(name = "daily_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyTask extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_log_id", nullable = false)
    private DailyLog dailyLog;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String taskContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private boolean isAdHoc = false;

    public boolean isCompleted() {
        return this.status == TaskStatus.COMPLETED;
    }

    public boolean isSkipped() {
        return this.status == TaskStatus.SKIPPED;
    }

    public boolean isPending() {
        return this.status == TaskStatus.PENDING;
    }
}
