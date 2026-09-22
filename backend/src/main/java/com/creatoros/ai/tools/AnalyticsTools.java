package com.creatoros.ai.tools;

import com.creatoros.analytics.AnalyticsService;
import com.creatoros.analytics.BrandAnalyticsService;
import com.creatoros.analytics.ExplainSpikeService;
import com.creatoros.analytics.GrowthPoint;
import com.creatoros.analytics.RegionAnalyticsService;
import com.creatoros.analytics.TopicAnalyticsService;
import com.creatoros.analytics.dto.BrandPerformanceResponse;
import com.creatoros.analytics.dto.ContentAnalyticsResponse;
import com.creatoros.analytics.dto.ExplanationResponse;
import com.creatoros.analytics.dto.PlatformPerformanceResponse;
import com.creatoros.analytics.dto.RegionPerformanceResponse;
import com.creatoros.analytics.dto.TopicPerformanceResponse;
import com.creatoros.analytics.dto.ViralEventResponse;
import com.creatoros.content.ContentService;
import com.creatoros.content.dto.ContentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Every CreatorOS analytics capability exposed as a deterministic, structured
 * tool (section 22). This is the *only* way both the standalone MCP server
 * and the in-app AI analyst ({@code AiAnalystService}) touch application
 * data - neither ever queries the database directly, so an LLM can only ever
 * ground its answer in numbers these already-tested services produced.
 *
 * <p>Every tool takes {@code userId} explicitly rather than reading it from
 * request/security context, since the standalone MCP server (external
 * clients, e.g. Claude Desktop) has no HTTP session to pull it from. The
 * in-app AI analyst supplies the authenticated caller's own id when it drives
 * these same tools - see docs/ai-and-mcp.md for the trust boundary this implies.
 */
@Component
public class AnalyticsTools {

    private final AnalyticsService analyticsService;
    private final TopicAnalyticsService topicAnalyticsService;
    private final RegionAnalyticsService regionAnalyticsService;
    private final BrandAnalyticsService brandAnalyticsService;
    private final ExplainSpikeService explainSpikeService;
    private final ContentService contentService;

    public AnalyticsTools(AnalyticsService analyticsService,
                           TopicAnalyticsService topicAnalyticsService,
                           RegionAnalyticsService regionAnalyticsService,
                           BrandAnalyticsService brandAnalyticsService,
                           ExplainSpikeService explainSpikeService,
                           ContentService contentService) {
        this.analyticsService = analyticsService;
        this.topicAnalyticsService = topicAnalyticsService;
        this.regionAnalyticsService = regionAnalyticsService;
        this.brandAnalyticsService = brandAnalyticsService;
        this.explainSpikeService = explainSpikeService;
        this.contentService = contentService;
    }

    @Tool(name = "get_content_metadata", description = "Get title, platform, topics, and publish info for one content item")
    public ContentResponse getContentMetadata(
            @ToolParam(description = "UUID of the content item") String contentId,
            @ToolParam(description = "UUID of the owning user") String userId) {
        return contentService.get(UUID.fromString(userId), UUID.fromString(contentId));
    }

    @Tool(name = "get_content_performance", description = "Get current totals, engagement rate, and latest growth/acceleration for one content item")
    public ContentAnalyticsResponse getContentPerformance(
            @ToolParam(description = "UUID of the content item") String contentId,
            @ToolParam(description = "UUID of the owning user") String userId) {
        return analyticsService.contentAnalytics(UUID.fromString(userId), UUID.fromString(contentId));
    }

    @Tool(name = "get_growth_timeline", description = "Get the full views/growth/acceleration/engagement timeline for one content item")
    public List<GrowthPoint> getGrowthTimeline(
            @ToolParam(description = "UUID of the content item") String contentId,
            @ToolParam(description = "UUID of the owning user") String userId) {
        return analyticsService.timeline(UUID.fromString(userId), UUID.fromString(contentId));
    }

    @Tool(name = "get_viral_events", description = "Get detected viral acceleration episodes for one content item, with evidence and confidence")
    public List<ViralEventResponse> getViralEvents(
            @ToolParam(description = "UUID of the content item") String contentId,
            @ToolParam(description = "UUID of the owning user") String userId) {
        return analyticsService.viralEvents(UUID.fromString(userId), UUID.fromString(contentId));
    }

    @Tool(name = "get_content_explanation", description = "Get an evidence-grounded explanation of why one content item performed the way it did")
    public ExplanationResponse getContentExplanation(
            @ToolParam(description = "UUID of the content item") String contentId,
            @ToolParam(description = "UUID of the owning user") String userId) {
        return explainSpikeService.explain(UUID.fromString(userId), UUID.fromString(contentId));
    }

    @Tool(name = "get_regional_performance", description = "Get performance aggregated by country/region across all of a user's content")
    public List<RegionPerformanceResponse> getRegionalPerformance(
            @ToolParam(description = "UUID of the user") String userId) {
        return regionAnalyticsService.regionPerformance(UUID.fromString(userId));
    }

    @Tool(name = "get_topic_performance", description = "Get performance aggregated by topic (e.g. football, gaming, AI) across all platforms")
    public List<TopicPerformanceResponse> getTopicPerformance(
            @ToolParam(description = "UUID of the user") String userId) {
        return topicAnalyticsService.topicPerformance(UUID.fromString(userId));
    }

    @Tool(name = "compare_platforms", description = "Compare performance across the user's connected platforms (e.g. YouTube vs Instagram)")
    public List<PlatformPerformanceResponse> comparePlatforms(
            @ToolParam(description = "UUID of the user") String userId) {
        return analyticsService.platforms(UUID.fromString(userId));
    }

    @Tool(name = "get_brand_patterns", description = "Get observed historical performance patterns for content that mentions specific brands")
    public List<BrandPerformanceResponse> getBrandPatterns(
            @ToolParam(description = "UUID of the user") String userId) {
        return brandAnalyticsService.brandPerformance(UUID.fromString(userId));
    }

    @Tool(name = "get_historical_patterns", description = "Get a combined summary of topic and platform performance patterns across the user's content history")
    public HistoricalPatternsResult getHistoricalPatterns(
            @ToolParam(description = "UUID of the user") String userId) {
        UUID id = UUID.fromString(userId);
        return new HistoricalPatternsResult(topicAnalyticsService.topicPerformance(id), analyticsService.platforms(id));
    }

    @Tool(name = "compare_content", description = "Compare current performance and growth between two content items")
    public ContentComparisonResult compareContent(
            @ToolParam(description = "UUID of the first content item") String contentIdA,
            @ToolParam(description = "UUID of the second content item") String contentIdB,
            @ToolParam(description = "UUID of the owning user") String userId) {
        UUID uid = UUID.fromString(userId);
        return new ContentComparisonResult(
                analyticsService.contentAnalytics(uid, UUID.fromString(contentIdA)),
                analyticsService.contentAnalytics(uid, UUID.fromString(contentIdB)));
    }

    public record HistoricalPatternsResult(List<TopicPerformanceResponse> byTopic, List<PlatformPerformanceResponse> byPlatform) {
    }

    public record ContentComparisonResult(ContentAnalyticsResponse contentA, ContentAnalyticsResponse contentB) {
    }
}
