package com.creatoros.analytics;

import java.time.Instant;

/**
 * One point on a content item's growth timeline, derived from a pair of
 * consecutive {@code content_snapshots} rows. Rates are per-hour so points
 * captured at uneven intervals remain comparable.
 */
public record GrowthPoint(
        Instant capturedAt,
        long views,
        long likes,
        long comments,
        long shares,
        double growthViewsPerHour,
        double accelerationViewsPerHour,
        double engagementRate
) {
}
