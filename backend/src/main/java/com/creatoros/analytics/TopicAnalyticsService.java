package com.creatoros.analytics;

import com.creatoros.analytics.dto.TopicPerformanceResponse;
import com.creatoros.content.ContentRegionMetric;
import com.creatoros.content.ContentRegionMetricRepository;
import com.creatoros.topic.ContentTopic;
import com.creatoros.topic.ContentTopicRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aggregates performance by topic across all connected platforms (section 12/13 analytics). */
@Service
public class TopicAnalyticsService {

    private final ContentTopicRepository contentTopicRepository;
    private final ContentRegionMetricRepository regionMetricRepository;
    private final AnalyticsQueryService analyticsQueryService;

    public TopicAnalyticsService(ContentTopicRepository contentTopicRepository,
                                  ContentRegionMetricRepository regionMetricRepository,
                                  AnalyticsQueryService analyticsQueryService) {
        this.contentTopicRepository = contentTopicRepository;
        this.regionMetricRepository = regionMetricRepository;
        this.analyticsQueryService = analyticsQueryService;
    }

    @Transactional(readOnly = true)
    public List<TopicPerformanceResponse> topicPerformance(UUID userId) {
        List<ContentTopic> contentTopics = contentTopicRepository.findByUserId(userId);
        if (contentTopics.isEmpty()) {
            return List.of();
        }

        Map<UUID, ContentWithSnapshot> latestByContent = analyticsQueryService.loadLatestForUser(userId).stream()
                .collect(Collectors.toMap(c -> c.content().getId(), c -> c));

        Map<UUID, List<ContentRegionMetric>> regionsByContent = regionMetricRepository
                .findByContent_SocialAccount_User_Id(userId).stream()
                .collect(Collectors.groupingBy(m -> m.getContent().getId()));

        Map<String, List<ContentTopic>> byTopic = contentTopics.stream()
                .collect(Collectors.groupingBy(ct -> ct.getTopic().getName()));

        return byTopic.entrySet().stream()
                .map(entry -> buildTopicPerformance(entry.getKey(), entry.getValue(), latestByContent, regionsByContent))
                .filter(r -> r.contentCount() > 0)
                .sorted(Comparator.comparingLong(TopicPerformanceResponse::totalViews).reversed())
                .toList();
    }

    private TopicPerformanceResponse buildTopicPerformance(String topicName, List<ContentTopic> members,
                                                             Map<UUID, ContentWithSnapshot> latestByContent,
                                                             Map<UUID, List<ContentRegionMetric>> regionsByContent) {
        List<ContentWithSnapshot> withData = members.stream()
                .map(ct -> latestByContent.get(ct.getContent().getId()))
                .filter(java.util.Objects::nonNull)
                .toList();

        long totalViews = withData.stream().mapToLong(c -> c.latest().getViews()).sum();
        double avgEngagement = withData.isEmpty() ? 0
                : withData.stream().mapToDouble(ContentWithSnapshot::engagementRate).average().orElse(0);
        Map<String, Long> viewsByPlatform = withData.stream()
                .collect(Collectors.groupingBy(c -> c.content().getPlatform().name(),
                        Collectors.summingLong(c -> c.latest().getViews())));

        Map<String, Long> viewsByCountry = new HashMap<>();
        for (ContentWithSnapshot c : withData) {
            for (ContentRegionMetric metric : regionsByContent.getOrDefault(c.content().getId(), List.of())) {
                viewsByCountry.merge(metric.getCountry(), metric.getViews(), Long::sum);
            }
        }
        String topRegion = viewsByCountry.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new TopicPerformanceResponse(topicName, withData.size(), totalViews, avgEngagement, viewsByPlatform, topRegion);
    }
}
