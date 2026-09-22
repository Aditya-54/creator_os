package com.creatoros.social.integration;

import java.time.Instant;
import java.util.Map;

/**
 * Normalized performance observation for one content item at one point in time,
 * as returned by a {@link SocialPlatformClient}. {@code regionViews} is an
 * optional lifetime-to-date regional breakdown (not historical), matching what
 * platform APIs typically expose.
 */
public record PlatformPerformanceSnapshot(
        String platformContentId,
        Instant capturedAt,
        long views,
        long likes,
        long comments,
        long shares,
        Long followersAttributed,
        Long watchTimeSeconds,
        Double avgViewDurationSeconds,
        Map<String, Long> regionViews
) {
}
