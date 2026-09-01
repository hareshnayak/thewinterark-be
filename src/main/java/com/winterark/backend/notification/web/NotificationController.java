package com.winterark.backend.notification.web;

import com.winterark.backend.auth.domain.User;
import com.winterark.backend.auth.domain.UserRepository;
import com.winterark.backend.notification.payload.PushSubscriptionRequestDTO;
import com.winterark.backend.notification.service.WebPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final WebPushService webPushService;
    private final com.winterark.backend.notification.service.PushNotificationService pushNotificationService;
    private final UserRepository userRepository;

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PushSubscriptionRequestDTO request) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        webPushService.subscribe(user, request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/test")
    public ResponseEntity<Void> sendTestNotification(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        pushNotificationService.sendNudge(user, "The Winter Ark");
        return ResponseEntity.ok().build();
    }
}
