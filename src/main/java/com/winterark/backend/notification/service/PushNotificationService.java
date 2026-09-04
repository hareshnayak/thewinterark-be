package com.winterark.backend.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.winterark.backend.auth.domain.User;
import com.winterark.backend.auth.domain.UserRepository;
import com.winterark.backend.notification.domain.PushSubscription;
import com.winterark.backend.notification.domain.PushSubscriptionRepository;
import com.winterark.backend.notification.payload.PushSubscriptionRequestDTO;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PushNotificationService {

    private final PushService pushService;
    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final FirebaseMessaging firebaseMessaging;

    @Autowired
    public PushNotificationService(
            PushService pushService,
            PushSubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            @Autowired(required = false) FirebaseMessaging firebaseMessaging) {
        this.pushService = pushService;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.firebaseMessaging = firebaseMessaging;
    }

    /**
     * Sends an FCM message using Firebase Admin SDK if token is an FCM token.
     */
    public boolean sendFcmMessage(String fcmToken, String title, String body) {
        if (firebaseMessaging == null) {
            log.info("FirebaseMessaging bean not configured, skipping direct FCM send.");
            return false;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("title", title)
                    .putData("body", body)
                    .putData("click_action", "/")
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("Successfully dispatched Firebase FCM notification! Message ID: {}", response);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send Firebase FCM notification to token {}: {}", fcmToken, e.getMessage());
            return false;
        }
    }

    /**
     * Encrypts and dispatches a Web Push notification to a target user's PushSubscription.
     */
    public void sendPushNotification(PushSubscription subscription, String title, String body) {
        String endpoint = subscription.getEndpoint();

        // 1. If endpoint is a raw FCM token or begins with "fcm:"
        if (endpoint != null && (endpoint.startsWith("fcm:") || !endpoint.startsWith("http"))) {
            String token = endpoint.startsWith("fcm:") ? endpoint.substring(4) : endpoint;
            boolean sent = sendFcmMessage(token, title, body);
            if (sent) return;
        }

        // 2. Standard W3C WebPush Protocol via PushService
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
            payload.put("icon", "/pwa-192x192.png");
            String payloadJson = objectMapper.writeValueAsString(payload);

            nl.martijndwars.webpush.Notification notification =
                    new nl.martijndwars.webpush.Notification(webPushSubscription, payloadJson);
            pushService.send(notification);
            log.info("Dispatched Web Push notification successfully to endpoint: {}", subscription.getEndpoint());
        } catch (Exception e) {
            log.error("Failed to send Web Push notification to subscription ID {}: {}", subscription.getId(), e.getMessage());
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

        String title = "Accountability Nudge! 🔥";
        String body = (senderUsername != null && !senderUsername.isBlank())
                ? "@" + senderUsername + " is nudging you to complete your routine!"
                : "Your accountability partner is nudging you to complete your routine!";

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
