package com.creatoros.analytics;

import com.creatoros.analytics.dto.ContentAnalyticsResponse;
import com.creatoros.analytics.dto.ContentSummary;
import com.creatoros.analytics.dto.OverviewResponse;
import com.creatoros.analytics.dto.PlatformPerformanceResponse;
import com.creatoros.analytics.dto.ViralEventResponse;
import com.creatoros.content.Content;
import com.creatoros.content.ContentRepository;
import com.creatoros.content.ContentSnapshotRepository;
import com.creatoros.exception.ResourceNotFoundException;
import com.creatoros.social.Platform;
import com.creatoros.topic.ContentTopicRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private final AnalyticsQueryService analyticsQueryService;
    private final ContentRepository contentRepository;
    private final ContentSnapshotRepository snapshotRepository;
    private final ContentTopicRepository contentTopicRepository;
    private final ViralEventRepository viralEventRepository;
    private final GrowthCalculator growthCalculator;
    private final ViralDetectionService viralDetectionService;

    public AnalyticsService(AnalyticsQueryService analyticsQueryService,
                             ContentRepository contentRepository,
                             ContentSnapshotRepository snapshotRepository,
                             ContentTopicRepository contentTopicRepository,
                             ViralEventRepository viralEventRepository,
                             GrowthCalculator growthCalculator,
                             ViralDetectionService viralDetectionService) {
        this.analyticsQueryService = analyticsQueryService;
        this.contentRepository = contentRepository;
        this.snapshotRepository = snapshotRepository;
        this.contentTopicRepository = contentTopicRepository;
        this.viralEventRepository = viralEventRepository;
        this.growthCalculator = growthCalculator;
        this.viralDetectionService = viralDetectionService;
    }

    @Transactional(readOnly = true)
    public OverviewResponse overview(UUID userId) {
        List<ContentWithSnapshot> latest = analyticsQueryService.loadLatestForUser(userId);

        long totalViews = latest.stream().mapToLong(c -> c.latest().getViews()).sum();
        long totalLikes = latest.stream().mapToLong(c -> c.latest().getLikes()).sum();
        long totalComments = latest.stream().mapToLong(c -> c.latest().getComments()).sum();
        long totalShares = latest.stream().mapToLong(c -> c.latest().getShares()).sum();
        double avgEngagementRate = latest.isEmpty() ? 0
                : latest.stream().mapToDouble(ContentWithSnapshot::engagementRate).average().orElse(0);

        List<ContentSummary> topContent = latest.stream()
                .sorted(Comparator.comparingLong((ContentWithSnapshot c) -> c.latest().getViews()).reversed())
                .limit(5)
                .map(c -> new ContentSummary(c.content().getId(), c.content().getTitle(),
                        c.content().getPlatform(), c.latest().getViews(), c.engagementRate()))
                .toList();

        Map<String, Long> platformBreakdown = latest.stream()
                .collect(Collectors.groupingBy(c -> c.content().getPlatform().name(),
                        Collectors.summingLong(c -> c.latest().getViews())));

        List<ViralEventResponse> recentViralEvents = viralEventRepository
                .findRecentForUser(userId, PageRequest.of(0, 5)).stream()
                .map(ViralEventResponse::from)
                .toList();

        return new OverviewResponse(contentRepository.countBySocialAccount_User_Id(userId),
                totalViews, totalLikes, totalComments, totalShares, avgEngagementRate,
                topContent, platformBreakdown, recentViralEvents);
    }

    @Transactional(readOnly = true)
    public ContentAnalyticsResponse contentAnalytics(UUID userId, UUID contentId) {
        Content content = requireOwned(userId, contentId);
        List<GrowthPoint> points = growthCalculator.computeSeries(
                snapshotRepository.findByContentIdOrderByCapturedAtAsc(contentId));
        GrowthPoint latest = points.isEmpty() ? null : points.get(points.size() - 1);

        List<String> topics = contentTopicRepository.findByContentId(contentId).stream()
                .map(ct -> ct.getTopic().getName())
                .toList();

        return new ContentAnalyticsResponse(
                contentId, content.getTitle(), content.getPlatform(),
                latest != null ? latest.views() : 0,
                latest != null ? latest.likes() : 0,
                latest != null ? latest.comments() : 0,
                latest != null ? latest.shares() : 0,
                latest != null ? latest.engagementRate() : 0,
                latest != null ? latest.growthViewsPerHour() : 0,
                latest != null ? latest.accelerationViewsPerHour() : 0,
                topics);
    }

    @Transactional(readOnly = true)
    public List<GrowthPoint> timeline(UUID userId, UUID contentId) {
        requireOwned(userId, contentId);
        return growthCalculator.computeSeries(snapshotRepository.findByContentIdOrderByCapturedAtAsc(contentId));
    }

    /** Detects viral episodes from the current timeline and idempotently upserts them, then returns the full history. */
    @Transactional
    public List<ViralEventResponse> viralEvents(UUID userId, UUID contentId) {
        Content content = requireOwned(userId, contentId);
        List<GrowthPoint> points = growthCalculator.computeSeries(
                snapshotRepository.findByContentIdOrderByCapturedAtAsc(contentId));
        List<ViralEventCandidate> candidates = viralDetectionService.detect(points);

        for (ViralEventCandidate candidate : candidates) {
            viralEventRepository.findByContentIdAndStartTime(contentId, candidate.startTime())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setEndTime(candidate.endTime());
                                existing.setPeakGrowth(candidate.peakGrowth());
                                existing.setBaselineGrowth(candidate.baselineGrowth());
                                existing.setMultiplier(candidate.multiplier());
                                existing.setConfidence(candidate.confidence());
                                existing.setExplanation(candidate.explanation());
                            },
                            () -> viralEventRepository.save(ViralEvent.builder()
                                    .content(content)
                                    .startTime(candidate.startTime())
                                    .endTime(candidate.endTime())
                                    .peakGrowth(candidate.peakGrowth())
                                    .baselineGrowth(candidate.baselineGrowth())
                                    .multiplier(candidate.multiplier())
                                    .confidence(candidate.confidence())
                                    .explanation(candidate.explanation())
                                    .build()));
        }

        return viralEventRepository.findByContentIdOrderByStartTimeDesc(contentId).stream()
                .map(ViralEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformPerformanceResponse> platforms(UUID userId) {
        List<ContentWithSnapshot> latest = analyticsQueryService.loadLatestForUser(userId);
        Map<Platform, List<ContentWithSnapshot>> byPlatform = latest.stream()
                .collect(Collectors.groupingBy(c -> c.content().getPlatform()));

        return byPlatform.entrySet().stream()
                .map(e -> new PlatformPerformanceResponse(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream().mapToLong(c -> c.latest().getViews()).sum(),
                        e.getValue().stream().mapToDouble(ContentWithSnapshot::engagementRate).average().orElse(0)))
                .sorted(Comparator.comparingLong(PlatformPerformanceResponse::totalViews).reversed())
                .toList();
    }

    private Content requireOwned(UUID userId, UUID contentId) {
        return contentRepository.findForUser(userId, contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
    }
}
