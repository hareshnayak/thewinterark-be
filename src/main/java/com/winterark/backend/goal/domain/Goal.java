package com.winterark.backend.goal.domain;

import com.winterark.backend.auth.domain.User;
import com.winterark.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = true)
    private String tagLine;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "goal_active_days", joinColumns = @JoinColumn(name = "goal_id"))
    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<DayOfWeek> activeDays = EnumSet.allOf(DayOfWeek.class);

    @Column(name = "timezone", nullable = false)
    @Builder.Default
    private String timezone = "UTC";
}
