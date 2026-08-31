package com.winterark.backend.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winterark.backend.auth.domain.User;
import com.winterark.backend.auth.domain.UserRepository;
import com.winterark.backend.notification.domain.PushSubscription;
import com.winterark.backend.notification.domain.PushSubscriptionRepository;
import com.winterark.backend.notification.payload.PushSubscriptionRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final PushService pushService;
    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Encrypts and dispatches a Web Push notification to a target user's PushSubscription.
     */
    public void sendPushNotification(PushSubscription subscription, String title, String body) {
        try {
            PushSubscriptionRequestDTO.Keys keys = objectMapper.readValue(
                    subscription.getKeysJson(),
                    PushSubscriptionRequestDTO.Keys.class
            );

            Subscription webPushSubscription = new Subscription(
                    subscription.getEndpoint(),
                    new Subscription.Keys(keys.getP256dh(), keys.getAuth())
            );

            Map<String, String> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("body", body);
            String payloadJson = objectMapper.writeValueAsString(payload);

            Notification notification = new Notification(webPushSubscription, payloadJson);
            pushService.send(notification);
            log.info("Dispatched Web Push notification successfully to endpoint: {}", subscription.getEndpoint());
        } catch (Exception e) {
            log.error("Failed to send Web Push notification to subscription ID {}: {}", subscription.getId(), e.getMessage(), e);
        }
    }

    /**
     * Sends an Accountability Nudge notification to all push subscriptions of the given target user.
     */
    public void sendNudge(User targetUser, String senderUsername) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserId(targetUser.getId());
        if (subscriptions.isEmpty()) {
            log.warn("No active push subscriptions found for user: {}", targetUser.getUsername());
            return;
        }

        String title = "Accountability Nudge!";
        String body = (senderUsername != null && !senderUsername.isBlank())
                ? senderUsername + " is reminding you to complete your daily goals!"
                : "Your friend is reminding you to complete your daily goals!";

        for (PushSubscription subscription : subscriptions) {
            sendPushNotification(subscription, title, body);
        }
    }

    /**
     * Resolves target user by ID and sends the Accountability Nudge.
     */
    public void sendNudgeByUserId(UUID targetUserId, String senderUsername) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + targetUserId));
        sendNudge(targetUser, senderUsername);
    }
}
