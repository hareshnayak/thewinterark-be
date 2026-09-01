package com.winterark.backend.social.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PendingFriendRequestDTO {
    private UUID requestId;
    private UUID requesterId;
    private String requesterUsername;
    private String requesterEmail;
    private LocalDateTime createdAt;
}
