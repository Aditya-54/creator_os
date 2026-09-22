package com.creatoros.demo;

import com.creatoros.content.ContentType;
import com.creatoros.social.Platform;
import java.util.Map;

/**
 * Hand-authored (not randomly seeded) demo content definition, so the six
 * analytical examples in docs/demo-script.md are reliably reproducible on
 * every seed rather than left to chance.
 */
record DemoContentSpec(
        Platform platform,
        String title,
        String description,
        ContentType contentType,
        Integer durationSeconds,
        int publishedHoursAgo,
        double basePotentialViews,
        double growthRate,
        double inflectionHour,
        double engagementRate,
        double shareRate,
        Map<String, Double> regionWeights
) {
}
