package com.creatoros.sync;

import com.creatoros.content.Content;
import com.creatoros.content.ContentRegionMetric;
import com.creatoros.content.ContentRegionMetricRepository;
import com.creatoros.content.ContentRepository;
import com.creatoros.content.ContentSnapshot;
import com.creatoros.content.ContentSnapshotRepository;
import com.creatoros.brand.Brand;
import com.creatoros.brand.BrandExtractionService;
import com.creatoros.brand.BrandRepository;
import com.creatoros.brand.ContentBrandMention;
import com.creatoros.brand.ContentBrandMentionRepository;
import com.creatoros.social.SocialAccount;
import com.creatoros.social.SocialAccountRepository;
import com.creatoros.social.SocialAccountStatus;
import com.creatoros.social.integration.PlatformContentItem;
import com.creatoros.social.integration.PlatformPerformanceSnapshot;
import com.creatoros.topic.ContentTopic;
import com.creatoros.topic.ContentTopicRepository;
import com.creatoros.topic.Topic;
import com.creatoros.topic.TopicExtractionService;
import com.creatoros.topic.TopicRepository;
import com.creatoros.topic.TopicSource;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists one sync cycle's results. Runs as a single short transaction that
 * only ever touches the database - all slow external I/O has already
 * happened by the time this is called (docs/transactions.md). Upserts are
 * keyed on the {@code (platform, platform_content_id)} unique constraint, so
 * calling this twice with the same input never creates duplicate rows
 * (docs/database.md "idempotent ingestion").
 */
@Service
public class ContentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ContentIngestionService.class);

    private final ContentRepository contentRepository;
    private final ContentSnapshotRepository snapshotRepository;
    private final ContentRegionMetricRepository regionMetricRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SyncJobRepository syncJobRepository;
    private final TopicRepository topicRepository;
    private final ContentTopicRepository contentTopicRepository;
    private final TopicExtractionService topicExtractionService;
    private final BrandRepository brandRepository;
    private final ContentBrandMentionRepository contentBrandMentionRepository;
    private final BrandExtractionService brandExtractionService;

    public ContentIngestionService(ContentRepository contentRepository,
                                    ContentSnapshotRepository snapshotRepository,
                                    ContentRegionMetricRepository regionMetricRepository,
                                    SocialAccountRepository socialAccountRepository,
                                    SyncJobRepository syncJobRepository,
                                    TopicRepository topicRepository,
                                    ContentTopicRepository contentTopicRepository,
                                    TopicExtractionService topicExtractionService,
                                    BrandRepository brandRepository,
                                    ContentBrandMentionRepository contentBrandMentionRepository,
                                    BrandExtractionService brandExtractionService) {
        this.contentRepository = contentRepository;
        this.snapshotRepository = snapshotRepository;
        this.regionMetricRepository = regionMetricRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.syncJobRepository = syncJobRepository;
        this.topicRepository = topicRepository;
        this.contentTopicRepository = contentTopicRepository;
        this.topicExtractionService = topicExtractionService;
        this.brandRepository = brandRepository;
        this.contentBrandMentionRepository = contentBrandMentionRepository;
        this.brandExtractionService = brandExtractionService;
    }

    @Transactional
    public void ingestAndComplete(SyncContext ctx, List<PlatformContentItem> items, List<PlatformPerformanceSnapshot> performance) {
        SocialAccount account = socialAccountRepository.getReferenceById(ctx.accountId());
        Instant now = Instant.now();

        Map<String, Content> byPlatformContentId = new HashMap<>();
        for (PlatformContentItem item : items) {
            Content content = contentRepository
                    .findByPlatformAndPlatformContentId(ctx.platform(), item.platformContentId())
                    .orElseGet(Content::new);
            content.setSocialAccount(account);
            content.setPlatform(ctx.platform());
            content.setPlatformContentId(item.platformContentId());
            content.setTitle(item.title());
            content.setDescription(item.description());
            content.setContentType(item.contentType());
            content.setPublishedAt(item.publishedAt());
            content.setDurationSeconds(item.durationSeconds());
            content.setLanguage(item.language());
            content.setCountry(item.country());
            content.setCategory(item.category());
            content.setThumbnailUrl(item.thumbnailUrl());
            content = contentRepository.save(content);
            byPlatformContentId.put(item.platformContentId(), content);

            assignTopics(content, item);
            assignBrandMentions(content, item);
        }

        int snapshotsWritten = 0;
        for (PlatformPerformanceSnapshot snapshot : performance) {
            Content content = byPlatformContentId.get(snapshot.platformContentId());
            if (content == null) {
                content = contentRepository
                        .findByPlatformAndPlatformContentId(ctx.platform(), snapshot.platformContentId())
                        .orElse(null);
            }
            if (content == null) {
                log.warn("Skipping performance snapshot for unknown content {}", snapshot.platformContentId());
                continue;
            }
            if (!snapshotRepository.existsByContentIdAndCapturedAt(content.getId(), snapshot.capturedAt())) {
                snapshotRepository.save(ContentSnapshot.builder()
                        .content(content)
                        .capturedAt(snapshot.capturedAt())
                        .views(snapshot.views())
                        .likes(snapshot.likes())
                        .comments(snapshot.comments())
                        .shares(snapshot.shares())
                        .followersAttributed(snapshot.followersAttributed())
                        .watchTimeSeconds(snapshot.watchTimeSeconds())
                        .avgViewDurationSeconds(snapshot.avgViewDurationSeconds())
                        .build());
                snapshotsWritten++;
            }
            upsertRegionMetrics(content, snapshot, now);
        }

        account.setLastSyncedAt(now);
        account.setStatus(SocialAccountStatus.CONNECTED);

        SyncJob job = syncJobRepository.getReferenceById(ctx.syncJobId());
        job.setStatus(SyncStatus.SUCCESS);
        job.setCompletedAt(now);

        log.info("Sync completed: account={} contentItems={} snapshotsWritten={}",
                ctx.accountId(), items.size(), snapshotsWritten);
    }

    private void assignTopics(Content content, PlatformContentItem item) {
        var topicNames = topicExtractionService.extract(
                item.title() != null ? item.title() : "",
                item.description() != null ? item.description() : "");
        for (String name : topicNames) {
            Topic topic = topicRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> topicRepository.save(Topic.builder().name(name).build()));
            if (contentTopicRepository.findByContentIdAndTopicId(content.getId(), topic.getId()).isEmpty()) {
                contentTopicRepository.save(ContentTopic.builder()
                        .content(content)
                        .topic(topic)
                        .source(TopicSource.RULE)
                        .build());
            }
        }
    }

    private void assignBrandMentions(Content content, PlatformContentItem item) {
        var brandNames = brandExtractionService.extract(
                item.title() != null ? item.title() : "",
                item.description() != null ? item.description() : "");
        for (String name : brandNames) {
            Brand brand = brandRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> brandRepository.save(Brand.builder().name(name).build()));
            if (contentBrandMentionRepository.findByContentIdAndBrandId(content.getId(), brand.getId()).isEmpty()) {
                contentBrandMentionRepository.save(ContentBrandMention.builder()
                        .content(content)
                        .brand(brand)
                        .mentionCount(1)
                        .build());
            }
        }
    }

    private void upsertRegionMetrics(Content content, PlatformPerformanceSnapshot snapshot, Instant now) {
        if (snapshot.regionViews() == null) {
            return;
        }
        for (var entry : snapshot.regionViews().entrySet()) {
            ContentRegionMetric metric = regionMetricRepository
                    .findByContentIdAndCountry(content.getId(), entry.getKey())
                    .orElseGet(ContentRegionMetric::new);
            metric.setContent(content);
            metric.setCountry(entry.getKey());
            metric.setViews(entry.getValue());
            metric.setEngagement(Math.round(entry.getValue() * 0.05));
            metric.setCapturedAt(now);
            regionMetricRepository.save(metric);
        }
    }
}
