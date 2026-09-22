package com.creatoros.analytics;

import java.time.Instant;

/**
 * A detected acceleration episode, before persistence. Deliberately carries a
 * {@code multiplier} and {@code confidence} label rather than a statistical
 * p-value - see docs/architecture.md on avoiding overclaimed certainty.
 */
public record ViralEventCandidate(
        Instant startTime,
        Instant endTime,
        double peakGrowth,
        double baselineGrowth,
        double multiplier,
        ViralConfidence confidence,
        String explanation
) {
}
