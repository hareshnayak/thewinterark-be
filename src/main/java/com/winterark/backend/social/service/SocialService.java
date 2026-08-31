package com.winterark.backend.social.service;

import com.winterark.backend.auth.domain.User;
import com.winterark.backend.auth.domain.UserRepository;
import com.winterark.backend.dailylog.domain.DailyLog;
import com.winterark.backend.dailylog.domain.DailyLogRepository;
import com.winterark.backend.dailylog.domain.DailyTask;
import com.winterark.backend.dailylog.domain.DailyTaskRepository;
import com.winterark.backend.goal.domain.Goal;
import com.winterark.backend.goal.domain.GoalRepository;
import com.winterark.backend.social.domain.Friendship;
import com.winterark.backend.social.domain.FriendshipRepository;
import com.winterark.backend.social.domain.GoalShare;
import com.winterark.backend.social.domain.GoalShareRepository;
import com.winterark.backend.social.payload.SharedGoalResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialService {

    private final GoalShareRepository goalShareRepository;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final DailyLogRepository dailyLogRepository;
    private final DailyTaskRepository dailyTaskRepository;

    @Transactional
    public void shareGoal(UUID goalId, UUID friendId, User currentUser) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        if (!goal.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Not your goal to share");
        }
        User friend = userRepository.findById(friendId).orElseThrow();

        // Check if friendship exists
        friendshipRepository.findByUserIdAndFriendId(currentUser.getId(), friendId)
                .orElseThrow(() -> new IllegalArgumentException("Must be friends to share goals"));

        GoalShare share = GoalShare.builder()
                .goal(goal)
                .friend(friend)
                .build();
        goalShareRepository.save(share);
    }

    @Transactional(readOnly = true)
    public List<SharedGoalResponseDTO> getFriendsGoals(UUID friendId) {
        List<GoalShare> shares = goalShareRepository.findByFriendId(friendId);
        List<SharedGoalResponseDTO> responses = new ArrayList<>();

        for (GoalShare share : shares) {
            Goal goal = share.getGoal();
            int progress = calculateTodayProgress(goal.getId());
            responses.add(SharedGoalResponseDTO.builder()
                    .goalId(goal.getId())
                    .title(goal.getTitle())
                    .todayProgressPercent(progress)
                    .build());
        }
        return responses;
    }

    private int calculateTodayProgress(UUID goalId) {
        LocalDate today = LocalDate.now();
        var logOpt = dailyLogRepository.findByGoalIdAndTargetDate(goalId, today);
        if (logOpt.isEmpty()) return 0;
        
        List<DailyTask> tasks = dailyTaskRepository.findByDailyLogId(logOpt.get().getId());
        if (tasks.isEmpty()) return 0;
        
        long completed = tasks.stream().filter(DailyTask::isCompleted).count();
        return (int) ((completed * 100.0f) / tasks.size());
    }
}
