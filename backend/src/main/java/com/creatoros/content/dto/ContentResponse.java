package com.creatoros.content.dto;

import com.creatoros.content.Content;
import com.creatoros.content.ContentType;
import com.creatoros.social.Platform;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentResponse(
        UUID id,
        Platform platform,
        String title,
        String description,
        ContentType contentType,
        Instant publishedAt,
        Integer durationSeconds,
        String language,
        String country,
        String category,
        String thumbnailUrl,
        boolean demo,
        List<String> topics
) {
    public static ContentResponse from(Content content, List<String> topics) {
        return new ContentResponse(
                content.getId(), content.getPlatform(), content.getTitle(), content.getDescription(),
                content.getContentType(), content.getPublishedAt(), content.getDurationSeconds(),
                content.getLanguage(), content.getCountry(), content.getCategory(), content.getThumbnailUrl(),
                content.isDemo(), topics);
    }
}
