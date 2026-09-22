package com.creatoros.social.dto;

import com.creatoros.social.Platform;
import com.creatoros.social.SocialAccount;
import com.creatoros.social.SocialAccountStatus;
import java.time.Instant;
import java.util.UUID;

public record SocialAccountResponse(
        UUID id,
        Platform platform,
        String username,
        SocialAccountStatus status,
        Instant connectedAt,
        Instant lastSyncedAt
) {
    public static SocialAccountResponse from(SocialAccount account) {
        return new SocialAccountResponse(
                account.getId(), account.getPlatform(), account.getUsername(),
                account.getStatus(), account.getConnectedAt(), account.getLastSyncedAt());
    }
}
