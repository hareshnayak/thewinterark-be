package com.winterark.backend.social.web;

import com.winterark.backend.auth.domain.User;
import com.winterark.backend.auth.domain.UserRepository;
import com.winterark.backend.notification.service.PushNotificationService;
import com.winterark.backend.social.payload.FriendRequestDTO;
import com.winterark.backend.social.payload.FriendResponseDTO;
import com.winterark.backend.social.payload.PendingFriendRequestDTO;
import com.winterark.backend.social.payload.SendFriendRequestDTO;
import com.winterark.backend.social.payload.SharedGoalResponseDTO;
import com.winterark.backend.social.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;
    private final PushNotificationService pushNotificationService;
    private final UserRepository userRepository;

    // --- Friend Requests & Connections ---

    @PostMapping("/friends/requests")
    public ResponseEntity<Void> sendFriendRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SendFriendRequestDTO request) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        socialService.sendFriendRequest(user, request.getTargetUserId());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/friends/requests/{userId}/accept")
    public ResponseEntity<Void> acceptFriendRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userId) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        socialService.acceptFriendRequest(user, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/friends/requests/{userId}")
    public ResponseEntity<Void> declineFriendRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID userId) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        socialService.declineFriendRequest(user, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/friends/requests/pending", "/friends/requests"})
    public ResponseEntity<List<PendingFriendRequestDTO>> getPendingRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(socialService.getPendingRequests(user));
    }

    @GetMapping("/friends")
    public ResponseEntity<List<FriendResponseDTO>> getAcceptedFriends(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(socialService.getAcceptedFriends(user));
    }

    // --- Goal Sharing & Permissions ---

    @PostMapping("/goals/{goalId}/share")
    public ResponseEntity<Void> shareGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID goalId,
            @RequestBody FriendRequestDTO request) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        socialService.shareGoal(goalId, request.getFriendId(), user);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/goals/{goalId}/share/{friendId}")
    public ResponseEntity<Void> revokeGoalAccess(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID goalId,
            @PathVariable UUID friendId) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        socialService.revokeGoalAccess(goalId, friendId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/goals/{goalId}/shares")
    public ResponseEntity<List<FriendResponseDTO>> getGoalShares(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID goalId) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(socialService.getGoalShares(goalId, user));
    }

    // --- Squad Progress & Nudges ---

    @GetMapping("/friends/{friendId}/goals")
    public ResponseEntity<List<SharedGoalResponseDTO>> getFriendsGoals(
            @PathVariable UUID friendId) {
        return ResponseEntity.ok(socialService.getFriendsGoals(friendId));
    }

    @GetMapping("/friends/feed")
    public ResponseEntity<List<SharedGoalResponseDTO>> getSquadFeed(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(socialService.getFriendsGoals(user.getId()));
    }

    @PostMapping("/goals/{goalId}/remind/{friendId}")
    public ResponseEntity<Void> remindFriend(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID goalId,
            @PathVariable UUID friendId) {
        String username = userDetails.getUsername();
        pushNotificationService.sendNudgeByUserId(friendId, username);
        return ResponseEntity.accepted().build();
    }
}
