package com.creatoros.analytics;

import com.creatoros.analytics.dto.ContentAnalyticsResponse;
import com.creatoros.analytics.dto.ExplanationResponse;
import com.creatoros.analytics.dto.OverviewResponse;
import com.creatoros.analytics.dto.PlatformPerformanceResponse;
import com.creatoros.analytics.dto.RegionPerformanceResponse;
import com.creatoros.analytics.dto.TopicPerformanceResponse;
import com.creatoros.analytics.dto.ViralEventResponse;
import com.creatoros.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final TopicAnalyticsService topicAnalyticsService;
    private final RegionAnalyticsService regionAnalyticsService;
    private final ExplainSpikeService explainSpikeService;

    public AnalyticsController(AnalyticsService analyticsService,
                                TopicAnalyticsService topicAnalyticsService,
                                RegionAnalyticsService regionAnalyticsService,
                                ExplainSpikeService explainSpikeService) {
        this.analyticsService = analyticsService;
        this.topicAnalyticsService = topicAnalyticsService;
        this.regionAnalyticsService = regionAnalyticsService;
        this.explainSpikeService = explainSpikeService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Cross-platform totals, top content, and recent viral events")
    public OverviewResponse overview(@AuthenticationPrincipal AuthenticatedUser principal) {
        return analyticsService.overview(principal.userId());
    }

    @GetMapping("/content/{id}")
    @Operation(summary = "Current totals and latest growth/acceleration for one content item")
    public ContentAnalyticsResponse contentAnalytics(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return analyticsService.contentAnalytics(principal.userId(), id);
    }

    @GetMapping("/content/{id}/timeline")
    @Operation(summary = "Full growth/acceleration timeline derived from historical snapshots")
    public List<GrowthPoint> timeline(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return analyticsService.timeline(principal.userId(), id);
    }

    @GetMapping("/content/{id}/viral-events")
    @Operation(summary = "Detected viral acceleration episodes for this content")
    public List<ViralEventResponse> viralEvents(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return analyticsService.viralEvents(principal.userId(), id);
    }

    @GetMapping("/content/{id}/explanation")
    @Operation(summary = "Evidence-grounded explanation of this content's performance (\"explain this spike\")")
    public ExplanationResponse explanation(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return explainSpikeService.explain(principal.userId(), id);
    }

    @GetMapping("/topics")
    @Operation(summary = "Performance aggregated by topic across all platforms")
    public List<TopicPerformanceResponse> topics(@AuthenticationPrincipal AuthenticatedUser principal) {
        return topicAnalyticsService.topicPerformance(principal.userId());
    }

    @GetMapping("/regions")
    @Operation(summary = "Performance aggregated by region/country")
    public List<RegionPerformanceResponse> regions(@AuthenticationPrincipal AuthenticatedUser principal) {
        return regionAnalyticsService.regionPerformance(principal.userId());
    }

    @GetMapping("/platforms")
    @Operation(summary = "Performance aggregated by platform")
    public List<PlatformPerformanceResponse> platforms(@AuthenticationPrincipal AuthenticatedUser principal) {
        return analyticsService.platforms(principal.userId());
    }
}
