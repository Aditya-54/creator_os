package com.creatoros.analytics.dto;

import java.util.Map;

public record TopicPerformanceResponse(
        String topic,
        long contentCount,
        long totalViews,
        double avgEngagementRate,
        Map<String, Long> viewsByPlatform,
        String topRegion
) {
}
