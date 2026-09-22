package com.creatoros.analytics;

import com.creatoros.content.ContentSnapshot;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Pure, side-effect-free growth/acceleration math (section 15 of the product
 * spec). Kept independent of persistence so it is trivially unit-testable and
 * reusable from both the REST layer and the MCP tools.
 *
 * <pre>
 * growth(t)       = metric(t) - metric(t-1), normalized to a per-hour rate
 * acceleration(t) = growth(t) - growth(t-1)
 * </pre>
 */
@Component
public class GrowthCalculator {

    private static final double MIN_INTERVAL_HOURS = 1.0 / 60.0; // guard divide-by-zero for sub-minute gaps

    /** Snapshots must already be sorted ascending by {@code capturedAt}. First point has zero growth/acceleration. */
    public List<GrowthPoint> computeSeries(List<ContentSnapshot> snapshots) {
        List<GrowthPoint> points = new ArrayList<>();
        if (snapshots.isEmpty()) {
            return points;
        }

        Double previousGrowth = null;
        for (int i = 0; i < snapshots.size(); i++) {
            ContentSnapshot current = snapshots.get(i);
            double growth = 0;
            if (i > 0) {
                ContentSnapshot prior = snapshots.get(i - 1);
                double hours = Math.max(MIN_INTERVAL_HOURS,
                        Duration.between(prior.getCapturedAt(), current.getCapturedAt()).toSeconds() / 3600.0);
                growth = (current.getViews() - prior.getViews()) / hours;
            }
            double acceleration = (i > 0 && previousGrowth != null) ? growth - previousGrowth : 0;
            double engagementRate = current.getViews() > 0
                    ? (current.getLikes() + current.getComments() + current.getShares()) / (double) current.getViews()
                    : 0;

            points.add(new GrowthPoint(current.getCapturedAt(), current.getViews(), current.getLikes(),
                    current.getComments(), current.getShares(), growth, acceleration, engagementRate));
            previousGrowth = growth;
        }
        return points;
    }

    /** Average growth rate excluding the most recent {@code excludeLast} points, used as a viral-detection baseline. */
    public double averageGrowth(List<GrowthPoint> points, int excludeLast) {
        int limit = Math.max(0, points.size() - excludeLast);
        if (limit == 0) {
            return 0;
        }
        return points.subList(0, limit).stream().mapToDouble(GrowthPoint::growthViewsPerHour).average().orElse(0);
    }
}
