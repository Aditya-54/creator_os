package com.creatoros.analytics.dto;

import com.creatoros.analytics.ViralConfidence;
import com.creatoros.analytics.ViralEvent;
import java.time.Instant;
import java.util.UUID;

public record ViralEventResponse(
        UUID id,
        UUID contentId,
        String contentTitle,
        Instant startTime,
        Instant endTime,
        double peakGrowth,
        double baselineGrowth,
        double multiplier,
        ViralConfidence confidence,
        String explanation
) {
    public static ViralEventResponse from(ViralEvent event) {
        return new ViralEventResponse(
                event.getId(), event.getContent().getId(), event.getContent().getTitle(),
                event.getStartTime(), event.getEndTime(), event.getPeakGrowth(), event.getBaselineGrowth(),
                event.getMultiplier(), event.getConfidence(), event.getExplanation());
    }
}
