package com.creatoros.analytics;

import com.creatoros.analytics.dto.BrandPerformanceResponse;
import com.creatoros.brand.ContentBrandMention;
import com.creatoros.brand.ContentBrandMentionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Observed historical patterns around brand mentions (section 14). Always
 * phrased as "content mentioning X has historically shown..." - never as a
 * claim about brand preference or intent, since the data only supports the
 * former.
 */
@Service
public class BrandAnalyticsService {

    private final ContentBrandMentionRepository contentBrandMentionRepository;
    private final AnalyticsQueryService analyticsQueryService;

    public BrandAnalyticsService(ContentBrandMentionRepository contentBrandMentionRepository,
                                  AnalyticsQueryService analyticsQueryService) {
        this.contentBrandMentionRepository = contentBrandMentionRepository;
        this.analyticsQueryService = analyticsQueryService;
    }

    @Transactional(readOnly = true)
    public List<BrandPerformanceResponse> brandPerformance(UUID userId) {
        List<ContentBrandMention> mentions = contentBrandMentionRepository.findByContent_SocialAccount_User_Id(userId);
        if (mentions.isEmpty()) {
            return List.of();
        }
        Map<UUID, ContentWithSnapshot> latestByContent = analyticsQueryService.loadLatestForUser(userId).stream()
                .collect(Collectors.toMap(c -> c.content().getId(), c -> c));

        return mentions.stream()
                .collect(Collectors.groupingBy(m -> m.getBrand().getName()))
                .entrySet().stream()
                .map(e -> buildBrandPerformance(e.getKey(), e.getValue(), latestByContent))
                .filter(r -> r.mentionCount() > 0)
                .sorted(Comparator.comparingLong(BrandPerformanceResponse::totalViews).reversed())
                .toList();
    }

    private BrandPerformanceResponse buildBrandPerformance(String brandName, List<ContentBrandMention> mentions,
                                                             Map<UUID, ContentWithSnapshot> latestByContent) {
        List<ContentWithSnapshot> withData = mentions.stream()
                .map(m -> latestByContent.get(m.getContent().getId()))
                .filter(java.util.Objects::nonNull)
                .toList();
        long totalViews = withData.stream().mapToLong(c -> c.latest().getViews()).sum();
        double avgEngagement = withData.isEmpty() ? 0
                : withData.stream().mapToDouble(ContentWithSnapshot::engagementRate).average().orElse(0);
        return new BrandPerformanceResponse(brandName, mentions.size(), totalViews, avgEngagement);
    }
}
