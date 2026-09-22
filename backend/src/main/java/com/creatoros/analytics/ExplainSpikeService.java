package com.creatoros.analytics;

import com.creatoros.analytics.dto.ExplanationResponse;
import com.creatoros.content.Content;
import com.creatoros.content.ContentRegionMetric;
import com.creatoros.content.ContentRegionMetricRepository;
import com.creatoros.content.ContentRepository;
import com.creatoros.content.ContentSnapshotRepository;
import com.creatoros.exception.ResourceNotFoundException;
import com.creatoros.topic.ContentTopic;
import com.creatoros.topic.ContentTopicRepository;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The signature "explain this spike" feature (section 17). Gathers actual
 * stored evidence - growth timeline, engagement change, regional breakdown,
 * topic, and similar historical content - and turns it into a structured,
 * evidence-grounded explanation. Rule-based and works with zero AI
 * configuration; {@code com.creatoros.ai.AiAnalystService} can optionally
 * rephrase/extend this same evidence with an LLM, but never replaces it.
 */
@Service
public class ExplainSpikeService {

    private final ContentRepository contentRepository;
    private final ContentSnapshotRepository snapshotRepository;
    private final ContentTopicRepository contentTopicRepository;
    private final ContentRegionMetricRepository regionMetricRepository;
    private final GrowthCalculator growthCalculator;
    private final ViralDetectionService viralDetectionService;

    public ExplainSpikeService(ContentRepository contentRepository,
                                ContentSnapshotRepository snapshotRepository,
                                ContentTopicRepository contentTopicRepository,
                                ContentRegionMetricRepository regionMetricRepository,
                                GrowthCalculator growthCalculator,
                                ViralDetectionService viralDetectionService) {
        this.contentRepository = contentRepository;
        this.snapshotRepository = snapshotRepository;
        this.contentTopicRepository = contentTopicRepository;
        this.regionMetricRepository = regionMetricRepository;
        this.growthCalculator = growthCalculator;
        this.viralDetectionService = viralDetectionService;
    }

    @Transactional(readOnly = true)
    public ExplanationResponse explain(UUID userId, UUID contentId) {
        Content content = contentRepository.findForUser(userId, contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));

        List<GrowthPoint> points = growthCalculator.computeSeries(
                snapshotRepository.findByContentIdOrderByCapturedAtAsc(contentId));
        List<ViralEventCandidate> candidates = viralDetectionService.detect(points);
        ViralEventCandidate top = candidates.stream()
                .max(Comparator.comparingDouble(ViralEventCandidate::multiplier))
                .orElse(null);

        List<String> evidence = new ArrayList<>();
        List<String> possibleFactors = new ArrayList<>();
        List<String> comparisons = new ArrayList<>();
        List<String> limitations = new ArrayList<>();

        if (top != null) {
            evidence.add(String.format("View velocity increased %.1fx (from ~%.0f to %.0f views/hour) starting around %s.",
                    top.multiplier(), top.baselineGrowth(), top.peakGrowth(), top.startTime()));
            possibleFactors.add("The data is consistent with a rapid increase in distribution or reach.");
        } else if (!points.isEmpty()) {
            evidence.add("No acceleration episode clearing the viral-detection threshold was found in the available history.");
        } else {
            evidence.add("No performance snapshots are available yet for this content.");
        }

        if (!points.isEmpty()) {
            GrowthPoint first = points.get(0);
            GrowthPoint last = points.get(points.size() - 1);
            if (last.engagementRate() > first.engagementRate()) {
                evidence.add(String.format("Engagement rate rose from %.1f%% to %.1f%% over the observed period.",
                        first.engagementRate() * 100, last.engagementRate() * 100));
            }
        }

        List<ContentRegionMetric> regions = regionMetricRepository.findByContentId(contentId);
        regions.stream()
                .max(Comparator.comparingLong(ContentRegionMetric::getViews))
                .ifPresent(top1 -> {
                    long totalRegionViews = regions.stream().mapToLong(ContentRegionMetric::getViews).sum();
                    double share = totalRegionViews > 0 ? (top1.getViews() * 100.0 / totalRegionViews) : 0;
                    evidence.add(String.format("%s accounts for the largest share of views observed so far (%.0f%% of regionally-attributed views).",
                            top1.getCountry(), share));
                });

        List<String> topics = contentTopicRepository.findByContentId(contentId).stream()
                .map(ct -> ct.getTopic().getName()).toList();
        if (!topics.isEmpty()) {
            ContentWithSnapshotViews similar = similarContentAverage(userId, contentId, topics.get(0), content.getPlatform().name());
            if (similar.count() > 0) {
                comparisons.add(String.format(
                        "Similar \"%s\" content on %s has historically averaged %.0f views (based on %d other item(s)).",
                        topics.get(0), content.getPlatform(), similar.averageViews(), similar.count()));
            }
        }

        if (content.getPublishedAt() != null) {
            var zoned = content.getPublishedAt().atZone(ZoneOffset.UTC);
            comparisons.add(String.format("Published on a %s at %02d:00 UTC.",
                    zoned.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH), zoned.getHour()));
        }

        limitations.add("This analysis is based only on the performance data CreatorOS has captured for this account; "
                + "it cannot see external factors (press coverage, algorithm changes, paid promotion, etc.).");
        limitations.add("Correlation between the observed signals and the timing of the acceleration does not establish "
                + "that any single factor caused it.");

        return new ExplanationResponse(contentId, top != null ? top.startTime() : null, top != null,
                evidence, possibleFactors, comparisons, limitations);
    }

    private record ContentWithSnapshotViews(long count, double averageViews) {
    }

    private ContentWithSnapshotViews similarContentAverage(UUID userId, UUID excludeContentId, String topicName, String platform) {
        List<ContentTopic> sameTopic = contentTopicRepository.findByUserId(userId).stream()
                .filter(ct -> ct.getTopic().getName().equals(topicName))
                .filter(ct -> !ct.getContent().getId().equals(excludeContentId))
                .filter(ct -> ct.getContent().getPlatform().name().equals(platform))
                .toList();
        if (sameTopic.isEmpty()) {
            return new ContentWithSnapshotViews(0, 0);
        }
        double avg = sameTopic.stream()
                .map(ct -> snapshotRepository.findFirstByContentIdOrderByCapturedAtDesc(ct.getContent().getId()))
                .flatMap(java.util.Optional::stream)
                .mapToLong(com.creatoros.content.ContentSnapshot::getViews)
                .average()
                .orElse(0);
        return new ContentWithSnapshotViews(sameTopic.size(), avg);
    }
}
