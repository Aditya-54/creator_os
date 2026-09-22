package com.creatoros.analytics.dto;

import java.util.List;
import java.util.Map;

public record OverviewResponse(
        long totalContent,
        long totalViews,
        long totalLikes,
        long totalComments,
        long totalShares,
        double avgEngagementRate,
        List<ContentSummary> topContent,
        Map<String, Long> platformBreakdown,
        List<ViralEventResponse> recentViralEvents
) {
}
