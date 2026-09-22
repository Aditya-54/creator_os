package com.creatoros.social.integration.mock;

import java.util.Random;

/**
 * Deterministic, seeded logistic growth curve used to synthesize believable
 * performance data for MOCK-platform accounts and the demo dataset. Being a
 * pure function of (seed, hoursSincePublish) means repeated calls - whether
 * from a live sync tick or from bulk historical seeding - always agree, which
 * is what keeps the mock adapter idempotent just like a real API would be.
 *
 * <p>~20% of content is marked "breakout": a much higher ceiling and a later,
 * sharper inflection point, which is what gives the viral detector something
 * real to find (see docs/demo-script.md example 5: delayed viral growth).
 */
public final class ContentGrowthModel {

    private final long seed;
    private final double basePotential;
    private final double growthRate;
    private final double inflectionHour;
    private final double engagementRate;
    private final double shareRate;
    private final boolean breakout;
    private final String[] regionWeights;
    private final double[] regionShares;

    private static final String[] REGIONS = {"US", "IN", "GB", "BR", "NG"};

    public ContentGrowthModel(String seedKey) {
        this.seed = seedKey.hashCode();
        Random rnd = new Random(seed);

        this.breakout = rnd.nextDouble() < 0.22;
        if (breakout) {
            this.basePotential = 40_000 + rnd.nextDouble() * 260_000;
            this.growthRate = 0.35 + rnd.nextDouble() * 0.4;
            this.inflectionHour = 10 + rnd.nextDouble() * 30;
        } else {
            this.basePotential = 800 + rnd.nextDouble() * 18_000;
            this.growthRate = 0.08 + rnd.nextDouble() * 0.15;
            this.inflectionHour = 2 + rnd.nextDouble() * 10;
        }
        this.engagementRate = 0.02 + rnd.nextDouble() * 0.08;
        this.shareRate = breakout ? 0.015 + rnd.nextDouble() * 0.03 : 0.003 + rnd.nextDouble() * 0.01;

        // Skew region distribution; occasionally one region dominates (regional intelligence demo).
        double[] weights = new double[REGIONS.length];
        double total = 0;
        int dominant = rnd.nextInt(REGIONS.length);
        for (int i = 0; i < REGIONS.length; i++) {
            weights[i] = rnd.nextDouble() * (i == dominant ? 4.0 : 1.0) + 0.1;
            total += weights[i];
        }
        this.regionShares = new double[REGIONS.length];
        for (int i = 0; i < REGIONS.length; i++) {
            regionShares[i] = weights[i] / total;
        }
        this.regionWeights = REGIONS;
    }

    public boolean isBreakout() {
        return breakout;
    }

    /** Cumulative views at {@code hoursSincePublish} (logistic growth, clamped to >= 0). */
    public long viewsAt(double hoursSincePublish) {
        if (hoursSincePublish < 0) {
            return 0;
        }
        double logistic = basePotential / (1 + Math.exp(-growthRate * (hoursSincePublish - inflectionHour)));
        return Math.round(logistic);
    }

    public long likesAt(double hoursSincePublish) {
        return Math.round(viewsAt(hoursSincePublish) * engagementRate);
    }

    public long commentsAt(double hoursSincePublish) {
        return Math.round(viewsAt(hoursSincePublish) * engagementRate * 0.12);
    }

    public long sharesAt(double hoursSincePublish) {
        return Math.round(viewsAt(hoursSincePublish) * shareRate);
    }

    public long followersAttributedAt(double hoursSincePublish) {
        return Math.round(viewsAt(hoursSincePublish) * 0.004);
    }

    public double avgViewDurationSeconds(int durationSeconds) {
        Random rnd = new Random(seed + 7);
        double retention = 0.35 + rnd.nextDouble() * 0.35;
        return Math.max(3.0, durationSeconds * retention);
    }

    public java.util.Map<String, Long> regionBreakdownAt(double hoursSincePublish) {
        long total = viewsAt(hoursSincePublish);
        java.util.Map<String, Long> result = new java.util.LinkedHashMap<>();
        for (int i = 0; i < regionWeights.length; i++) {
            result.put(regionWeights[i], Math.round(total * regionShares[i]));
        }
        return result;
    }
}
