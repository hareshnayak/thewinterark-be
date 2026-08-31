package com.winterark.backend.social.payload;

import lombok.Data;
import java.util.UUID;

@Data
public class FriendRequestDTO {
    private UUID friendId;
}
