package com.creatoros.analytics;

import com.creatoros.analytics.dto.RegionPerformanceResponse;
import com.creatoros.content.ContentRegionMetric;
import com.creatoros.content.ContentRegionMetricRepository;
import com.creatoros.topic.ContentTopicRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aggregates performance by region/country, never inventing regions the platform APIs didn't supply (section 13). */
@Service
public class RegionAnalyticsService {

    private final ContentRegionMetricRepository regionMetricRepository;
    private final ContentTopicRepository contentTopicRepository;

    public RegionAnalyticsService(ContentRegionMetricRepository regionMetricRepository,
                                   ContentTopicRepository contentTopicRepository) {
        this.regionMetricRepository = regionMetricRepository;
        this.contentTopicRepository = contentTopicRepository;
    }

    @Transactional(readOnly = true)
    public List<RegionPerformanceResponse> regionPerformance(UUID userId) {
        List<ContentRegionMetric> metrics = regionMetricRepository.findByContent_SocialAccount_User_Id(userId);
        if (metrics.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<String>> topicsByContent = contentTopicRepository.findByUserId(userId).stream()
                .collect(Collectors.groupingBy(ct -> ct.getContent().getId(),
                        Collectors.mapping(ct -> ct.getTopic().getName(), Collectors.toList())));

        Map<String, List<ContentRegionMetric>> byCountry = metrics.stream()
                .collect(Collectors.groupingBy(ContentRegionMetric::getCountry));

        return byCountry.entrySet().stream()
                .map(entry -> buildRegionPerformance(entry.getKey(), entry.getValue(), topicsByContent))
                .sorted(Comparator.comparingLong(RegionPerformanceResponse::totalViews).reversed())
                .toList();
    }

    private RegionPerformanceResponse buildRegionPerformance(String country, List<ContentRegionMetric> metrics,
                                                               Map<UUID, List<String>> topicsByContent) {
        long totalViews = metrics.stream().mapToLong(ContentRegionMetric::getViews).sum();
        long totalEngagement = metrics.stream().mapToLong(ContentRegionMetric::getEngagement).sum();

        Map<String, Long> viewsByTopic = new HashMap<>();
        for (ContentRegionMetric metric : metrics) {
            for (String topic : topicsByContent.getOrDefault(metric.getContent().getId(), List.of())) {
                viewsByTopic.merge(topic, metric.getViews(), Long::sum);
            }
        }
        String topTopic = viewsByTopic.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        return new RegionPerformanceResponse(country, totalViews, totalEngagement, topTopic);
    }
}
