package com.creatoros.social.integration;

import com.creatoros.content.ContentType;
import java.time.Instant;

/** Normalized content metadata as returned by a {@link SocialPlatformClient}. */
public record PlatformContentItem(
        String platformContentId,
        String title,
        String description,
        ContentType contentType,
        Instant publishedAt,
        Integer durationSeconds,
        String language,
        String country,
        String category,
        String thumbnailUrl
) {
}
