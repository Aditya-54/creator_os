package com.creatoros.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatoros.content.ContentSnapshot;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViralDetectionServiceTest {

    private final GrowthCalculator growthCalculator = new GrowthCalculator();
    private final ViralDetectionService viralDetectionService = new ViralDetectionService(growthCalculator);

    private ContentSnapshot snapshot(Instant capturedAt, long views) {
        return ContentSnapshot.builder().capturedAt(capturedAt).views(views).likes(0).comments(0).shares(0).build();
    }

    @Test
    void noEventsForSteadyLinearGrowth() {
        Instant t0 = Instant.now();
        List<ContentSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            snapshots.add(snapshot(t0.plus(i, ChronoUnit.HOURS), 1000L + i * 100L));
        }
        List<GrowthPoint> points = growthCalculator.computeSeries(snapshots);

        assertThat(viralDetectionService.detect(points)).isEmpty();
    }

    @Test
    void detectsSuddenAccelerationAboveBaseline() {
        Instant t0 = Instant.now();
        List<ContentSnapshot> snapshots = new ArrayList<>();
        // Steady baseline growth of ~100 views/hr for 5 hours...
        long views = 1000;
        for (int i = 0; i <= 5; i++) {
            snapshots.add(snapshot(t0.plus(i, ChronoUnit.HOURS), views));
            views += 100;
        }
        // ...then a sudden 20x acceleration.
        for (int i = 6; i <= 8; i++) {
            snapshots.add(snapshot(t0.plus(i, ChronoUnit.HOURS), views));
            views += 5000;
        }

        List<GrowthPoint> points = growthCalculator.computeSeries(snapshots);
        List<ViralEventCandidate> events = viralDetectionService.detect(points);

        assertThat(events).isNotEmpty();
        ViralEventCandidate event = events.get(0);
        assertThat(event.multiplier()).isGreaterThanOrEqualTo(ViralDetectionService.MULTIPLIER_THRESHOLD);
        assertThat(event.peakGrowth()).isGreaterThan(event.baselineGrowth());
        assertThat(event.confidence()).isIn(ViralConfidence.MEDIUM, ViralConfidence.HIGH);
        assertThat(event.explanation()).isNotBlank();
    }

    @Test
    void ignoresDramaticRatioOnTinyAbsoluteNumbers() {
        // Growth from 1 view/hr to 5 views/hr is a 5x ratio but tiny in absolute terms - should not flag.
        Instant t0 = Instant.now();
        List<ContentSnapshot> snapshots = List.of(
                snapshot(t0, 0),
                snapshot(t0.plus(1, ChronoUnit.HOURS), 1),
                snapshot(t0.plus(2, ChronoUnit.HOURS), 2),
                snapshot(t0.plus(3, ChronoUnit.HOURS), 3),
                snapshot(t0.plus(4, ChronoUnit.HOURS), 8));

        List<GrowthPoint> points = growthCalculator.computeSeries(snapshots);
        assertThat(viralDetectionService.detect(points)).isEmpty();
    }

    @Test
    void tooFewPointsNeverFlags() {
        Instant t0 = Instant.now();
        List<GrowthPoint> points = growthCalculator.computeSeries(List.of(
                snapshot(t0, 0), snapshot(t0.plus(1, ChronoUnit.HOURS), 100000)));

        assertThat(viralDetectionService.detect(points)).isEmpty();
    }
}
