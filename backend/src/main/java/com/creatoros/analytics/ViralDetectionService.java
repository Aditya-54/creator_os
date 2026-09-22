package com.creatoros.analytics;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Deterministic, explainable viral-acceleration detector (section 16). A
 * point is flagged when its growth rate both (a) exceeds a multiple of the
 * content's own prior average growth and (b) clears an absolute floor, so a
 * tiny account going from 2 to 8 views/hour is not mislabeled "viral" just
 * because the ratio looks dramatic. Flagged points are grouped into
 * contiguous episodes. This is a heuristic, not a statistical test - the
 * generated explanation always says so (see {@link #buildExplanation}).
 */
@Service
public class ViralDetectionService {

    static final double MULTIPLIER_THRESHOLD = 3.0;
    static final double MIN_ABSOLUTE_GROWTH_PER_HOUR = 50.0;
    static final double RATIO_EPSILON = 1.0;
    private static final int MIN_HISTORY_FOR_BASELINE = 3;

    private final GrowthCalculator growthCalculator;

    public ViralDetectionService(GrowthCalculator growthCalculator) {
        this.growthCalculator = growthCalculator;
    }

    public List<ViralEventCandidate> detect(List<GrowthPoint> points) {
        List<ViralEventCandidate> events = new ArrayList<>();
        if (points.size() <= MIN_HISTORY_FOR_BASELINE) {
            return events;
        }

        int runStart = -1;
        double runBaseline = 0;
        double runPeak = Double.NEGATIVE_INFINITY;

        for (int i = MIN_HISTORY_FOR_BASELINE; i < points.size(); i++) {
            double baseline = growthCalculator.averageGrowth(points.subList(0, i + 1), 1);
            double current = points.get(i).growthViewsPerHour();
            double multiplier = current / Math.max(baseline, RATIO_EPSILON);
            boolean flagged = multiplier >= MULTIPLIER_THRESHOLD && current >= MIN_ABSOLUTE_GROWTH_PER_HOUR;

            if (flagged) {
                if (runStart == -1) {
                    runStart = i;
                    runBaseline = baseline;
                    runPeak = current;
                } else {
                    runPeak = Math.max(runPeak, current);
                }
            } else if (runStart != -1) {
                events.add(buildCandidate(points, runStart, i - 1, runBaseline, runPeak));
                runStart = -1;
            }
        }
        if (runStart != -1) {
            events.add(buildCandidate(points, runStart, points.size() - 1, runBaseline, runPeak));
        }
        return events;
    }

    private ViralEventCandidate buildCandidate(List<GrowthPoint> points, int startIdx, int endIdx,
                                                double baseline, double peak) {
        double multiplier = peak / Math.max(baseline, RATIO_EPSILON);
        ViralConfidence confidence = multiplier >= 10 ? ViralConfidence.HIGH
                : multiplier >= 5 ? ViralConfidence.MEDIUM : ViralConfidence.LOW;
        var start = points.get(startIdx);
        var end = points.get(endIdx);
        return new ViralEventCandidate(
                start.capturedAt(), end.capturedAt(), peak, baseline, multiplier, confidence,
                buildExplanation(multiplier, baseline, peak));
    }

    private String buildExplanation(double multiplier, double baseline, double peak) {
        return String.format(
                "View velocity reached %.0f views/hour, about %.1fx the prior baseline of %.0f views/hour. "
                        + "This pattern is consistent with a rapid increase in distribution; it does not by itself "
                        + "prove what caused it.",
                peak, multiplier, baseline);
    }
}
