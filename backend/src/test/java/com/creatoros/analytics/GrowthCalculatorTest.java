package com.creatoros.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatoros.content.ContentSnapshot;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class GrowthCalculatorTest {

    private final GrowthCalculator calculator = new GrowthCalculator();

    private ContentSnapshot snapshot(Instant capturedAt, long views, long likes, long comments, long shares) {
        return ContentSnapshot.builder()
                .capturedAt(capturedAt).views(views).likes(likes).comments(comments).shares(shares)
                .build();
    }

    @Test
    void emptySeriesReturnsEmptyList() {
        assertThat(calculator.computeSeries(List.of())).isEmpty();
    }

    @Test
    void firstPointHasZeroGrowthAndAcceleration() {
        Instant t0 = Instant.now();
        List<GrowthPoint> points = calculator.computeSeries(List.of(snapshot(t0, 1000, 10, 2, 1)));

        assertThat(points).hasSize(1);
        assertThat(points.get(0).growthViewsPerHour()).isZero();
        assertThat(points.get(0).accelerationViewsPerHour()).isZero();
    }

    @Test
    void computesGrowthPerHourBetweenConsecutiveSnapshots() {
        Instant t0 = Instant.now();
        Instant t1 = t0.plus(1, ChronoUnit.HOURS);
        List<GrowthPoint> points = calculator.computeSeries(List.of(
                snapshot(t0, 1000, 0, 0, 0),
                snapshot(t1, 1500, 0, 0, 0)));

        assertThat(points.get(1).growthViewsPerHour()).isEqualTo(500.0);
    }

    @Test
    void computesAccelerationAsChangeInGrowthRate() {
        Instant t0 = Instant.now();
        Instant t1 = t0.plus(1, ChronoUnit.HOURS);
        Instant t2 = t1.plus(1, ChronoUnit.HOURS);
        // growth t1 = 500/hr, growth t2 = 2000/hr -> acceleration = 1500/hr
        List<GrowthPoint> points = calculator.computeSeries(List.of(
                snapshot(t0, 1000, 0, 0, 0),
                snapshot(t1, 1500, 0, 0, 0),
                snapshot(t2, 3500, 0, 0, 0)));

        assertThat(points.get(2).growthViewsPerHour()).isEqualTo(2000.0);
        assertThat(points.get(2).accelerationViewsPerHour()).isEqualTo(1500.0);
    }

    @Test
    void engagementRateIsZeroWhenViewsAreZero() {
        List<GrowthPoint> points = calculator.computeSeries(List.of(snapshot(Instant.now(), 0, 5, 1, 1)));
        assertThat(points.get(0).engagementRate()).isZero();
    }

    @Test
    void engagementRateComputedFromLikesCommentsShares() {
        List<GrowthPoint> points = calculator.computeSeries(List.of(snapshot(Instant.now(), 100, 5, 3, 2)));
        assertThat(points.get(0).engagementRate()).isEqualTo(0.10);
    }

    @Test
    void handlesSubMinuteGapsWithoutDivideByZero() {
        Instant t0 = Instant.now();
        Instant t1 = t0.plusMillis(500);
        List<GrowthPoint> points = calculator.computeSeries(List.of(
                snapshot(t0, 1000, 0, 0, 0),
                snapshot(t1, 1001, 0, 0, 0)));

        assertThat(points.get(1).growthViewsPerHour()).isFinite();
    }

    @Test
    void averageGrowthExcludesTrailingPoints() {
        Instant t0 = Instant.now();
        List<GrowthPoint> points = calculator.computeSeries(List.of(
                snapshot(t0, 0, 0, 0, 0),
                snapshot(t0.plus(1, ChronoUnit.HOURS), 100, 0, 0, 0),
                snapshot(t0.plus(2, ChronoUnit.HOURS), 200, 0, 0, 0),
                snapshot(t0.plus(3, ChronoUnit.HOURS), 10000, 0, 0, 0)));

        double baseline = calculator.averageGrowth(points, 1);

        assertThat(baseline).isEqualTo((0 + 100 + 100) / 3.0);
    }
}
