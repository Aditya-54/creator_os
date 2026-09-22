package com.creatoros.analytics.dto;

public record BrandPerformanceResponse(String brand, long mentionCount, long totalViews, double avgEngagementRate) {
}
