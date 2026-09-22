package com.creatoros.content;

import com.creatoros.common.BaseEntity;
import com.creatoros.social.Platform;
import com.creatoros.social.SocialAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single piece of published content on a platform. The
 * {@code (platform, platformContentId)} unique constraint is what makes
 * ingestion idempotent - re-syncing the same video/post upserts this row
 * instead of duplicating it (see docs/transactions.md).
 */
@Entity
@Table(name = "content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Content extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "social_account_id", nullable = false)
    private SocialAccount socialAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Platform platform;

    @Column(name = "platform_content_id", nullable = false)
    private String platformContentId;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 32)
    private ContentType contentType;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    private String language;

    private String country;

    private String category;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "is_demo", nullable = false)
    @Builder.Default
    private boolean demo = false;
}
