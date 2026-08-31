package com.winterark.backend.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winterark.backend.auth.domain.User;
import com.winterark.backend.notification.domain.PushSubscription;
import com.winterark.backend.notification.domain.PushSubscriptionRepository;
import com.winterark.backend.notification.payload.PushSubscriptionRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    private final PushService pushService;
    private final PushSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void subscribe(User user, PushSubscriptionRequestDTO request) {
        try {
            String keysJson = objectMapper.writeValueAsString(request.getKeys());
            PushSubscription subscription = PushSubscription.builder()
                    .user(user)
                    .endpoint(request.getEndpoint())
                    .keysJson(keysJson)
                    .build();
            subscriptionRepository.save(subscription);
        } catch (Exception e) {
            log.error("Failed to save subscription", e);
            throw new RuntimeException("Could not subscribe to web push");
        }
    }

    public void sendNotification(UUID userId, String title, String body) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserId(userId);
        for (PushSubscription sub : subscriptions) {
            try {
                PushSubscriptionRequestDTO.Keys keys = objectMapper.readValue(
                        sub.getKeysJson(),
                        PushSubscriptionRequestDTO.Keys.class
                );

                Subscription webPushSub = new Subscription(
                        sub.getEndpoint(),
                        new Subscription.Keys(keys.getP256dh(), keys.getAuth())
                );

                String payload = String.format("{\"title\":\"%s\",\"body\":\"%s\"}", title, body);
                Notification notification = new Notification(webPushSub, payload);

                pushService.send(notification);
                log.info("Push notification sent to user {}", userId);
            } catch (Exception e) {
                log.error("Failed to send push notification to user {}", userId, e);
            }
        }
    }
}
