package com.winterark.backend.social.service;

import com.winterark.backend.auth.domain.User;
import com.winterark.backend.auth.domain.UserRepository;
import com.winterark.backend.dailylog.domain.DailyLog;
import com.winterark.backend.dailylog.domain.DailyLogRepository;
import com.winterark.backend.dailylog.domain.DailyTask;
import com.winterark.backend.dailylog.domain.DailyTaskRepository;
import com.winterark.backend.goal.domain.Goal;
import com.winterark.backend.goal.domain.GoalRepository;
import com.winterark.backend.goal.domain.PredefinedTaskRepository;
import com.winterark.backend.social.domain.Friendship;
import com.winterark.backend.social.domain.FriendshipRepository;
import com.winterark.backend.social.domain.GoalShare;
import com.winterark.backend.social.domain.GoalShareRepository;
import com.winterark.backend.social.payload.FriendResponseDTO;
import com.winterark.backend.social.payload.PendingFriendRequestDTO;
import com.winterark.backend.social.payload.SharedGoalResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialService {

    private final GoalShareRepository goalShareRepository;
    private final GoalRepository goalRepository;
    private final PredefinedTaskRepository predefinedTaskRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final DailyLogRepository dailyLogRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final com.winterark.backend.goal.service.GoalService goalService;

    @Transactional
    public void sendFriendRequest(User currentUser, UUID targetUserId) {
        if (currentUser.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(currentUser.getId(), targetUserId);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == Friendship.FriendshipStatus.ACCEPTED) {
                throw new IllegalArgumentException("Already friends with this user");
            }
            if (f.getUser().getId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("Friend request already pending");
            }
            // Target user already sent request to current user -> auto-accept
            f.setStatus(Friendship.FriendshipStatus.ACCEPTED);
            friendshipRepository.save(f);
            return;
        }

        Friendship friendship = Friendship.builder()
                .user(currentUser)
                .friend(targetUser)
                .status(Friendship.FriendshipStatus.PENDING)
                .build();
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void acceptFriendRequest(User currentUser, UUID requesterId) {
        Friendship friendship = friendshipRepository.findByUserIdAndFriendId(requesterId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Pending friend request not found"));

        friendship.setStatus(Friendship.FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void declineFriendRequest(User currentUser, UUID requesterId) {
        Friendship friendship = friendshipRepository.findByUserIdAndFriendId(requesterId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public List<PendingFriendRequestDTO> getPendingRequests(User currentUser) {
        List<Friendship> pendings = friendshipRepository.findByFriendIdAndStatus(currentUser.getId(), Friendship.FriendshipStatus.PENDING);
        return pendings.stream()
                .map(f -> PendingFriendRequestDTO.builder()
                        .requestId(f.getId())
                        .requesterId(f.getUser().getId())
                        .requesterUsername(f.getUser().getUsername())
                        .requesterEmail(f.getUser().getEmail())
                        .createdAt(f.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendResponseDTO> getAcceptedFriends(User currentUser) {
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(currentUser.getId());
        return friendships.stream()
                .map(f -> {
                    User other = f.getUser().getId().equals(currentUser.getId()) ? f.getFriend() : f.getUser();
                    return FriendResponseDTO.builder()
                            .id(other.getId())
                            .username(other.getUsername())
                            .email(other.getEmail())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void shareGoal(UUID goalId, UUID friendId, User currentUser) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        if (!goal.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Not your goal to share");
        }
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("Friend not found"));

        // Check if friendship is ACCEPTED
        if (!friendshipRepository.areFriends(currentUser.getId(), friendId)) {
            throw new IllegalArgumentException("Must be accepted friends to share goals");
        }

        if (goalShareRepository.existsByGoalIdAndFriendId(goalId, friendId)) {
            return;
        }

        GoalShare share = GoalShare.builder()
                .goal(goal)
                .friend(friend)
                .build();
        goalShareRepository.save(share);
    }

    @Transactional
    public void revokeGoalAccess(UUID goalId, UUID friendId, User currentUser) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        if (!goal.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Not your goal to modify sharing permissions");
        }

        goalShareRepository.deleteByGoalIdAndFriendId(goalId, friendId);
    }

    @Transactional(readOnly = true)
    public List<FriendResponseDTO> getGoalShares(UUID goalId, User currentUser) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found"));
        if (!goal.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Not your goal");
        }

        List<GoalShare> shares = goalShareRepository.findByGoalId(goalId);
        return shares.stream()
                .map(s -> FriendResponseDTO.builder()
                        .id(s.getFriend().getId())
                        .username(s.getFriend().getUsername())
                        .email(s.getFriend().getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SharedGoalResponseDTO> getFriendsGoals(UUID friendId) {
        List<GoalShare> shares = goalShareRepository.findByFriendId(friendId);
        List<SharedGoalResponseDTO> responses = new ArrayList<>();

        for (GoalShare share : shares) {
            Goal goal = share.getGoal();
            if (goal.isArchived()) continue;
            int progress = calculateTodayProgress(goal.getId());
            int streak = goalService.getGoalStreak(goal.getId()).getCurrentStreak();
            int total = getTodayTotalTasks(goal.getId());
            int completed = getTodayCompletedTasks(goal.getId());

            responses.add(SharedGoalResponseDTO.builder()
                    .goalId(goal.getId())
                    .title(goal.getTitle())
                    .todayProgressPercent(progress)
                    .ownerId(goal.getUser().getId())
                    .ownerUsername(goal.getUser().getUsername())
                    .totalTasks(total)
                    .completedTasks(completed)
                    .streakDays(streak)
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

    private int getTodayTotalTasks(UUID goalId) {
        LocalDate today = LocalDate.now();
        var logOpt = dailyLogRepository.findByGoalIdAndTargetDate(goalId, today);
        if (logOpt.isPresent()) {
            List<DailyTask> tasks = dailyTaskRepository.findByDailyLogId(logOpt.get().getId());
            if (!tasks.isEmpty()) return tasks.size();
        }
        return predefinedTaskRepository.findByGoalId(goalId).size();
    }

    private int getTodayCompletedTasks(UUID goalId) {
        LocalDate today = LocalDate.now();
        var logOpt = dailyLogRepository.findByGoalIdAndTargetDate(goalId, today);
        if (logOpt.isEmpty()) return 0;
        List<DailyTask> tasks = dailyTaskRepository.findByDailyLogId(logOpt.get().getId());
        return (int) tasks.stream().filter(DailyTask::isCompleted).count();
    }

    private int calculateStreak(UUID goalId) {
        LocalDate today = LocalDate.now();
        int streak = 0;
        for (int i = 0; i < 90; i++) {
            LocalDate d = today.minusDays(i);
            var logOpt = dailyLogRepository.findByGoalIdAndTargetDate(goalId, d);
            if (logOpt.isEmpty()) {
                if (i == 0) continue; // Today might not be finished yet
                break;
            }
            List<DailyTask> tasks = dailyTaskRepository.findByDailyLogId(logOpt.get().getId());
            if (tasks.isEmpty()) break;
            boolean allDone = tasks.stream().allMatch(DailyTask::isCompleted);
            if (allDone) {
                streak++;
            } else {
                if (i == 0) continue;
                break;
            }
        }
        return streak;
    }
}
