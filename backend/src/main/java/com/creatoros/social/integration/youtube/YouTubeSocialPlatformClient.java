package com.creatoros.social.integration.youtube;

import com.creatoros.content.ContentType;
import com.creatoros.social.Platform;
import com.creatoros.social.integration.ConnectedAccount;
import com.creatoros.social.integration.PlatformContentItem;
import com.creatoros.social.integration.PlatformPerformanceSnapshot;
import com.creatoros.social.integration.PlatformProfile;
import com.creatoros.social.integration.SocialConnectRequest;
import com.creatoros.social.integration.SocialPlatformClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Real integration with the public YouTube Data API v3, using an API key
 * (no OAuth). This covers channel lookup, video metadata, and the public
 * statistics (views/likes/comments) that are readable without the channel
 * owner's authorization.
 *
 * <p><b>Known limitation:</b> share count, watch time, average view duration,
 * and regional view breakdowns are only exposed via the YouTube Analytics API,
 * which requires OAuth authorization from the channel owner. Those fields are
 * left {@code null}/{@code 0} here rather than fabricated. Wiring up the OAuth
 * flow (Google client id/secret are already in configuration) would let a
 * future {@code fetchPerformance} call populate them - see docs/architecture.md.
 */
@Component
@EnableConfigurationProperties(YouTubeProperties.class)
public class YouTubeSocialPlatformClient implements SocialPlatformClient {

    private static final Logger log = LoggerFactory.getLogger(YouTubeSocialPlatformClient.class);
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";

    private final RestClient restClient;
    private final YouTubeProperties properties;

    public YouTubeSocialPlatformClient(RestClient.Builder restClientBuilder, YouTubeProperties properties) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
        this.properties = properties;
    }

    @Override
    public Platform platform() {
        return Platform.YOUTUBE;
    }

    @Override
    public PlatformProfile fetchAccountProfile(SocialConnectRequest request) {
        requireConfigured();
        String handle = normalizeHandle(request.handle());
        JsonNode channel = getSingleItem("/channels", "part", "snippet", "forHandle", handle);
        String channelId = channel.path("id").asText();
        String title = channel.path("snippet").path("title").asText(handle);
        return new PlatformProfile(channelId, handle, title);
    }

    @Override
    public List<PlatformContentItem> fetchContent(ConnectedAccount account) {
        requireConfigured();
        String uploadsPlaylistId = resolveUploadsPlaylistId(account.platformAccountId());
        List<String> videoIds = fetchUploadedVideoIds(uploadsPlaylistId);
        if (videoIds.isEmpty()) {
            return List.of();
        }
        JsonNode videos = getItems("/videos", "part", "snippet,contentDetails", "id", String.join(",", videoIds));

        List<PlatformContentItem> items = new ArrayList<>();
        for (JsonNode video : videos) {
            JsonNode snippet = video.path("snippet");
            String isoDuration = video.path("contentDetails").path("duration").asText(null);
            Integer durationSeconds = parseDurationSeconds(isoDuration);
            ContentType type = (durationSeconds != null && durationSeconds <= 60) ? ContentType.SHORT : ContentType.VIDEO;
            items.add(new PlatformContentItem(
                    video.path("id").asText(),
                    snippet.path("title").asText(null),
                    snippet.path("description").asText(null),
                    type,
                    parseInstant(snippet.path("publishedAt").asText(null)),
                    durationSeconds,
                    snippet.path("defaultLanguage").asText(null),
                    null,
                    snippet.path("categoryId").asText(null),
                    snippet.path("thumbnails").path("high").path("url").asText(null)
            ));
        }
        return items;
    }

    @Override
    public List<PlatformPerformanceSnapshot> fetchPerformance(ConnectedAccount account, List<String> platformContentIds) {
        requireConfigured();
        if (platformContentIds.isEmpty()) {
            return List.of();
        }
        Instant now = Instant.now();
        List<PlatformPerformanceSnapshot> snapshots = new ArrayList<>();
        // YouTube Data API allows up to 50 ids per request.
        for (List<String> batch : partition(platformContentIds, 50)) {
            JsonNode videos = getItems("/videos", "part", "statistics", "id", String.join(",", batch));
            for (JsonNode video : videos) {
                JsonNode stats = video.path("statistics");
                snapshots.add(new PlatformPerformanceSnapshot(
                        video.path("id").asText(),
                        now,
                        stats.path("viewCount").asLong(0),
                        stats.path("likeCount").asLong(0),
                        stats.path("commentCount").asLong(0),
                        0L, // share count is not exposed by the public Data API
                        null, // requires YouTube Analytics API (OAuth)
                        null, // requires YouTube Analytics API (OAuth)
                        null, // requires YouTube Analytics API (OAuth)
                        null  // regional breakdown requires YouTube Analytics API (OAuth)
                ));
            }
        }
        return snapshots;
    }

    private String resolveUploadsPlaylistId(String channelId) {
        JsonNode channel = getSingleItem("/channels", "part", "contentDetails", "id", channelId);
        return channel.path("contentDetails").path("relatedPlaylists").path("uploads").asText();
    }

    private List<String> fetchUploadedVideoIds(String uploadsPlaylistId) {
        JsonNode items = getItems("/playlistItems", "part", "contentDetails", "playlistId", uploadsPlaylistId, "maxResults", "50");
        List<String> ids = new ArrayList<>();
        for (JsonNode item : items) {
            ids.add(item.path("contentDetails").path("videoId").asText());
        }
        return ids;
    }

    private JsonNode getSingleItem(String path, String... params) {
        JsonNode items = getItems(path, params);
        if (items.size() == 0) {
            throw new YouTubeIntegrationException("YouTube API returned no results for " + path);
        }
        return items.get(0);
    }

    private JsonNode getItems(String path, String... params) {
        try {
            JsonNode body = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path(path).queryParam("key", properties.apiKey());
                        for (int i = 0; i + 1 < params.length; i += 2) {
                            builder = builder.queryParam(params[i], params[i + 1]);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
            return body != null ? body.path("items") : com.fasterxml.jackson.databind.node.MissingNode.getInstance();
        } catch (RestClientException e) {
            log.warn("YouTube Data API call to {} failed: {}", path, e.getMessage());
            throw new YouTubeIntegrationException("YouTube Data API call failed: " + path, e);
        }
    }

    private void requireConfigured() {
        if (!properties.configured()) {
            throw new YouTubeIntegrationException(
                    "YOUTUBE_API_KEY is not configured; connect a MOCK account or set the key to use real YouTube data");
        }
    }

    private static String normalizeHandle(String handle) {
        if (handle == null || handle.isBlank()) {
            throw new YouTubeIntegrationException("A channel handle is required to connect a YouTube account");
        }
        return handle.startsWith("@") ? handle : "@" + handle;
    }

    private static Integer parseDurationSeconds(String isoDuration) {
        if (isoDuration == null || isoDuration.isBlank()) {
            return null;
        }
        try {
            return (int) Duration.parse(isoDuration).getSeconds();
        } catch (Exception e) {
            return null;
        }
    }

    private static Instant parseInstant(String value) {
        return value != null ? Instant.parse(value) : null;
    }

    private static List<List<String>> partition(List<String> list, int size) {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
