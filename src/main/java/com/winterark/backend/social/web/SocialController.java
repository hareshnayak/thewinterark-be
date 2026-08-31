package com.winterark.backend.social.web;

import com.winterark.backend.auth.domain.User;
import com.winterark.backend.auth.domain.UserRepository;
import com.winterark.backend.notification.service.PushNotificationService;
import com.winterark.backend.social.payload.FriendRequestDTO;
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

    @PostMapping("/goals/{goalId}/share")
    public ResponseEntity<Void> shareGoal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID goalId,
            @RequestBody FriendRequestDTO request) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        socialService.shareGoal(goalId, request.getFriendId(), user);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/friends/{friendId}/goals")
    public ResponseEntity<List<SharedGoalResponseDTO>> getFriendsGoals(
            @PathVariable UUID friendId) {
        return ResponseEntity.ok(socialService.getFriendsGoals(friendId));
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
