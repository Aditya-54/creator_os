package com.creatoros.analytics.dto;

import com.creatoros.social.Platform;

public record PlatformPerformanceResponse(Platform platform, long contentCount, long totalViews, double avgEngagementRate) {
}
