package com.winterark.backend.notification.payload;

import lombok.Data;

@Data
public class PushSubscriptionRequestDTO {
    private String endpoint;
    private Keys keys;

    @Data
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}
