package com.creatoros.analytics.dto;

import com.creatoros.social.Platform;
import java.util.UUID;

public record ContentSummary(UUID id, String title, Platform platform, long views, double engagementRate) {
}
