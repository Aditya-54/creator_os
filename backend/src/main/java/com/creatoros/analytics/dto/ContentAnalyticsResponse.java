package com.creatoros.analytics.dto;

import com.creatoros.social.Platform;
import java.util.List;
import java.util.UUID;

public record ContentAnalyticsResponse(
        UUID contentId,
        String title,
        Platform platform,
        long totalViews,
        long totalLikes,
        long totalComments,
        long totalShares,
        double engagementRate,
        double latestGrowthPerHour,
        double latestAccelerationPerHour,
        List<String> topics
) {
}
