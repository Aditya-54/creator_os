package com.creatoros.demo;

import com.creatoros.brand.Brand;
import com.creatoros.brand.BrandExtractionService;
import com.creatoros.brand.BrandRepository;
import com.creatoros.brand.ContentBrandMention;
import com.creatoros.brand.ContentBrandMentionRepository;
import com.creatoros.content.Content;
import com.creatoros.content.ContentRegionMetric;
import com.creatoros.content.ContentRegionMetricRepository;
import com.creatoros.content.ContentRepository;
import com.creatoros.content.ContentSnapshot;
import com.creatoros.content.ContentSnapshotRepository;
import com.creatoros.content.ContentType;
import com.creatoros.social.Platform;
import com.creatoros.social.SocialAccount;
import com.creatoros.social.SocialAccountRepository;
import com.creatoros.social.SocialAccountStatus;
import com.creatoros.topic.ContentTopic;
import com.creatoros.topic.ContentTopicRepository;
import com.creatoros.topic.Topic;
import com.creatoros.topic.TopicExtractionService;
import com.creatoros.topic.TopicRepository;
import com.creatoros.topic.TopicSource;
import com.creatoros.user.User;
import com.creatoros.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a rich, deterministic demo workspace so CreatorOS is immediately
 * demonstrable without any live social API credentials (section 35 of the
 * product spec). Every row this writes is tagged {@code is_demo = true} and
 * every demo account uses a {@code demo-*} platform id, so it is always
 * possible to tell synthetic data apart from a real connected account, and
 * to cleanly remove it later. Bypasses the sync pipeline entirely (this is a
 * bulk one-time seed, not the hot ingestion path) and writes a full
 * historical snapshot series per item directly, which the sync worker never
 * does in one call.
 */
@Service
public class DemoDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoDataGenerator.class);
    private static final String DEMO_PREFIX = "demo-";

    private static final Map<String, Double> INDIA_HEAVY = Map.of("IN", 0.65, "US", 0.15, "GB", 0.10, "BR", 0.05, "NG", 0.05);
    private static final Map<String, Double> US_HEAVY = Map.of("US", 0.50, "GB", 0.15, "IN", 0.15, "BR", 0.10, "NG", 0.10);
    private static final Map<String, Double> BALANCED = Map.of("US", 0.25, "IN", 0.25, "GB", 0.20, "BR", 0.15, "NG", 0.15);

    private final List<DemoContentSpec> catalog = List.of(
            // Example 5: delayed viral growth - slow for ~40h then a sharp breakout.
            new DemoContentSpec(Platform.YOUTUBE, "Last-minute winner in the derby! 🔥 ft. Red Bull energy",
                    "Injury time drama in the biggest football derby of the season.", ContentType.SHORT, 45,
                    72, 180_000, 0.5, 40, 0.07, 0.025, INDIA_HEAVY),
            // Example 3: football performs disproportionately well in India.
            new DemoContentSpec(Platform.YOUTUBE, "Breaking down the 4-3-3 press",
                    "Tactical analysis of high-press football systems.", ContentType.VIDEO, 620,
                    96, 45_000, 0.15, 10, 0.05, 0.01, INDIA_HEAVY),
            // Example 6 (a): the "hit" of two visually similar posts.
            new DemoContentSpec(Platform.YOUTUBE, "Training vlog: matchday prep 🔥",
                    "Behind the scenes before kickoff.", ContentType.SHORT, 20,
                    48, 95_000, 0.4, 15, 0.06, 0.02, BALANCED),
            // Example 6 (b): the "miss" - same topic/format/timing, far lower performance.
            new DemoContentSpec(Platform.YOUTUBE, "Street skills tutorial: first touch drills",
                    "Basic footwork drills for beginners.", ContentType.SHORT, 22,
                    47, 3_200, 0.1, 6, 0.04, 0.005, BALANCED),
            // Example 2 (a): gaming, strong on YouTube.
            new DemoContentSpec(Platform.YOUTUBE, "Ranked #1 to Grandmaster in 48 hours",
                    "Full ranked grind VOD with commentary.", ContentType.VIDEO, 900,
                    60, 120_000, 0.25, 12, 0.08, 0.015, US_HEAVY),
            // Example 2 (b): same topic, weak on Instagram.
            new DemoContentSpec(Platform.INSTAGRAM, "[DEMO] Gaming highlights reel",
                    "Quick highlights from this week's ranked grind.", ContentType.REEL, 28,
                    58, 4_200, 0.1, 8, 0.025, 0.004, US_HEAVY),
            // Example 4: high views, deliberately low engagement.
            new DemoContentSpec(Platform.YOUTUBE, "I built an AI agent that codes itself",
                    "A walkthrough of an autonomous coding agent.", ContentType.VIDEO, 940,
                    80, 210_000, 0.2, 20, 0.006, 0.001, US_HEAVY),
            new DemoContentSpec(Platform.YOUTUBE, "GPT vs Claude: honest comparison",
                    "Head to head comparison across coding, writing, and reasoning tasks.", ContentType.VIDEO, 560,
                    50, 68_000, 0.2, 10, 0.05, 0.012, US_HEAVY),
            new DemoContentSpec(Platform.INSTAGRAM, "[DEMO] Unboxing the new flagship phone ft. Samsung",
                    "First impressions of the latest flagship release.", ContentType.REEL, 35,
                    40, 52_000, 0.3, 9, 0.045, 0.01, BALANCED),
            new DemoContentSpec(Platform.YOUTUBE, "5 productivity apps I can't live without",
                    "A quick roundup of daily-driver productivity apps.", ContentType.SHORT, 55,
                    30, 15_000, 0.2, 5, 0.04, 0.008, US_HEAVY),
            new DemoContentSpec(Platform.INSTAGRAM, "[DEMO] Street style haul ft. Nike x Adidas",
                    "New pickups for the season.", ContentType.REEL, 40,
                    36, 38_000, 0.25, 8, 0.06, 0.012, BALANCED),
            new DemoContentSpec(Platform.INSTAGRAM, "[DEMO] How to style one blazer 5 ways",
                    "Five outfit combinations from a single blazer.", ContentType.POST, null,
                    20, 9_000, 0.15, 4, 0.05, 0.007, BALANCED),
            new DemoContentSpec(Platform.YOUTUBE, "Index funds explained in 60 seconds",
                    "A beginner-friendly breakdown of index fund investing.", ContentType.SHORT, 58,
                    15, 22_000, 0.3, 4, 0.055, 0.015, US_HEAVY),
            new DemoContentSpec(Platform.YOUTUBE, "Top 10 anime plot twists of the decade",
                    "Counting down the most shocking anime moments.", ContentType.VIDEO, 780,
                    65, 54_000, 0.18, 12, 0.06, 0.014, BALANCED)
    );

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final ContentRepository contentRepository;
    private final ContentSnapshotRepository snapshotRepository;
    private final ContentRegionMetricRepository regionMetricRepository;
    private final TopicRepository topicRepository;
    private final ContentTopicRepository contentTopicRepository;
    private final TopicExtractionService topicExtractionService;
    private final BrandRepository brandRepository;
    private final ContentBrandMentionRepository contentBrandMentionRepository;
    private final BrandExtractionService brandExtractionService;

    public DemoDataGenerator(UserRepository userRepository,
                              SocialAccountRepository socialAccountRepository,
                              ContentRepository contentRepository,
                              ContentSnapshotRepository snapshotRepository,
                              ContentRegionMetricRepository regionMetricRepository,
                              TopicRepository topicRepository,
                              ContentTopicRepository contentTopicRepository,
                              TopicExtractionService topicExtractionService,
                              BrandRepository brandRepository,
                              ContentBrandMentionRepository contentBrandMentionRepository,
                              BrandExtractionService brandExtractionService) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.contentRepository = contentRepository;
        this.snapshotRepository = snapshotRepository;
        this.regionMetricRepository = regionMetricRepository;
        this.topicRepository = topicRepository;
        this.contentTopicRepository = contentTopicRepository;
        this.topicExtractionService = topicExtractionService;
        this.brandRepository = brandRepository;
        this.contentBrandMentionRepository = contentBrandMentionRepository;
        this.brandExtractionService = brandExtractionService;
    }

    @Transactional
    public void seedForUser(UUID userId) {
        User user = userRepository.getReferenceById(userId);
        SocialAccount youtube = findOrCreateDemoAccount(user, Platform.YOUTUBE, "demo-" + userId + "-yt");
        SocialAccount instagram = findOrCreateDemoAccount(user, Platform.INSTAGRAM, "demo-" + userId + "-ig");

        Instant now = Instant.now();
        int index = 0;
        for (DemoContentSpec spec : catalog) {
            SocialAccount account = spec.platform() == Platform.YOUTUBE ? youtube : instagram;
            seedOne(account, spec, "demo-" + userId + "-" + index, now);
            index++;
        }
        log.info("Seeded {} demo content items for user {}", catalog.size(), userId);
    }

    @Transactional
    public void resetForUser(UUID userId) {
        List<SocialAccount> demoAccounts = socialAccountRepository.findByUserId(userId).stream()
                .filter(a -> a.getPlatformAccountId().startsWith(DEMO_PREFIX))
                .toList();
        socialAccountRepository.deleteAll(demoAccounts);
    }

    private SocialAccount findOrCreateDemoAccount(User user, Platform platform, String platformAccountId) {
        return socialAccountRepository.findByUserId(user.getId()).stream()
                .filter(a -> a.getPlatformAccountId().equals(platformAccountId))
                .findFirst()
                .orElseGet(() -> socialAccountRepository.save(SocialAccount.builder()
                        .user(user)
                        .platform(platform)
                        .platformAccountId(platformAccountId)
                        .username("@demo." + platform.name().toLowerCase())
                        .connectedAt(Instant.now())
                        .lastSyncedAt(Instant.now())
                        .status(SocialAccountStatus.CONNECTED)
                        .build()));
    }

    private void seedOne(SocialAccount account, DemoContentSpec spec, String platformContentId, Instant now) {
        Instant publishedAt = now.minus(spec.publishedHoursAgo(), ChronoUnit.HOURS);

        Content content = contentRepository.findByPlatformAndPlatformContentId(spec.platform(), platformContentId)
                .orElseGet(Content::new);
        content.setSocialAccount(account);
        content.setPlatform(spec.platform());
        content.setPlatformContentId(platformContentId);
        content.setTitle(spec.title());
        content.setDescription(spec.description());
        content.setContentType(spec.contentType());
        content.setPublishedAt(publishedAt);
        content.setDurationSeconds(spec.durationSeconds());
        content.setLanguage("en");
        content.setCountry(topRegion(spec.regionWeights()));
        content.setCategory(null);
        content.setDemo(true);
        content = contentRepository.save(content);

        assignTopics(content, spec);
        assignBrands(content, spec);

        List<Double> hourOffsets = buildHourOffsets(spec.publishedHoursAgo());
        for (double hour : hourOffsets) {
            Instant capturedAt = publishedAt.plusSeconds(Math.round(hour * 3600));
            if (snapshotRepository.existsByContentIdAndCapturedAt(content.getId(), capturedAt)) {
                continue;
            }
            long views = logisticViews(spec, hour);
            long likes = Math.round(views * spec.engagementRate());
            long comments = Math.round(views * spec.engagementRate() * 0.12);
            long shares = Math.round(views * spec.shareRate());
            Long watchTimeSeconds = spec.durationSeconds() != null ? Math.round(views * spec.durationSeconds() * 0.4) : null;
            Double avgViewDuration = spec.durationSeconds() != null ? spec.durationSeconds() * 0.4 : null;

            snapshotRepository.save(ContentSnapshot.builder()
                    .content(content)
                    .capturedAt(capturedAt)
                    .views(views)
                    .likes(likes)
                    .comments(comments)
                    .shares(shares)
                    .followersAttributed(Math.round(views * 0.004))
                    .watchTimeSeconds(watchTimeSeconds)
                    .avgViewDurationSeconds(avgViewDuration)
                    .build());
        }

        long finalViews = logisticViews(spec, spec.publishedHoursAgo());
        for (var entry : spec.regionWeights().entrySet()) {
            long regionViews = Math.round(finalViews * entry.getValue());
            ContentRegionMetric metric = regionMetricRepository
                    .findByContentIdAndCountry(content.getId(), entry.getKey())
                    .orElseGet(ContentRegionMetric::new);
            metric.setContent(content);
            metric.setCountry(entry.getKey());
            metric.setViews(regionViews);
            metric.setEngagement(Math.round(regionViews * spec.engagementRate()));
            metric.setCapturedAt(now);
            regionMetricRepository.save(metric);
        }
    }

    /** Hourly resolution for the first 48h (where the interesting dynamics happen), then every 4h after. */
    private List<Double> buildHourOffsets(int publishedHoursAgo) {
        List<Double> hours = new ArrayList<>();
        for (int h = 0; h <= Math.min(publishedHoursAgo, 48); h++) {
            hours.add((double) h);
        }
        for (int h = 52; h <= publishedHoursAgo; h += 4) {
            hours.add((double) h);
        }
        if (hours.isEmpty() || hours.get(hours.size() - 1) < publishedHoursAgo) {
            hours.add((double) publishedHoursAgo);
        }
        return hours;
    }

    private long logisticViews(DemoContentSpec spec, double hour) {
        double value = spec.basePotentialViews() / (1 + Math.exp(-spec.growthRate() * (hour - spec.inflectionHour())));
        return Math.round(value);
    }

    private String topRegion(Map<String, Double> weights) {
        return weights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("US");
    }

    private void assignTopics(Content content, DemoContentSpec spec) {
        var topicNames = topicExtractionService.extract(spec.title(), spec.description());
        for (String name : topicNames) {
            Topic topic = topicRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> topicRepository.save(Topic.builder().name(name).build()));
            if (contentTopicRepository.findByContentIdAndTopicId(content.getId(), topic.getId()).isEmpty()) {
                contentTopicRepository.save(ContentTopic.builder()
                        .content(content).topic(topic).source(TopicSource.RULE).build());
            }
        }
    }

    private void assignBrands(Content content, DemoContentSpec spec) {
        var brandNames = brandExtractionService.extract(spec.title(), spec.description());
        for (String name : brandNames) {
            Brand brand = brandRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> brandRepository.save(Brand.builder().name(name).build()));
            if (contentBrandMentionRepository.findByContentIdAndBrandId(content.getId(), brand.getId()).isEmpty()) {
                contentBrandMentionRepository.save(ContentBrandMention.builder()
                        .content(content).brand(brand).mentionCount(1).build());
            }
        }
    }
}
