package com.creatoros.analytics.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Structured, evidence-first answer to "explain this spike" (section 17).
 * Deliberately separates observed {@code evidence} from interpretive
 * {@code possibleFactors} and always includes {@code limitations} so the
 * response never implies proven causality.
 */
public record ExplanationResponse(
        UUID contentId,
        Instant turningPoint,
        boolean viralEventDetected,
        List<String> evidence,
        List<String> possibleFactors,
        List<String> comparisons,
        List<String> limitations
) {
}
