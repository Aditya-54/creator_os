package com.creatoros.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatoros.content.Content;
import com.creatoros.content.ContentRepository;
import com.creatoros.content.ContentSnapshotRepository;
import com.creatoros.content.ContentType;
import com.creatoros.social.Platform;
import com.creatoros.social.SocialAccount;
import com.creatoros.social.SocialAccountRepository;
import com.creatoros.social.SocialAccountStatus;
import com.creatoros.social.integration.PlatformContentItem;
import com.creatoros.social.integration.PlatformPerformanceSnapshot;
import com.creatoros.support.AbstractIntegrationTest;
import com.creatoros.user.User;
import com.creatoros.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies section 26's idempotency requirement directly against the real
 * unique constraints: running the same ingestion twice must upsert the same
 * {@code content} row (never duplicate it), and must not duplicate a snapshot
 * captured at the same instant.
 */
class ContentIngestionServiceIdempotencyTest extends AbstractIntegrationTest {

    @Autowired
    private ContentIngestionService ingestionService;
    @Autowired
    private AccountLockService accountLockService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SocialAccountRepository socialAccountRepository;
    @Autowired
    private ContentRepository contentRepository;
    @Autowired
    private ContentSnapshotRepository snapshotRepository;

    @Test
    void reingestingTheSameContentDoesNotDuplicateRows() {
        UUID accountId = createConnectedAccount();
        String platformContentId = "idempotency-test-" + UUID.randomUUID();
        Instant capturedAt = Instant.now();

        List<PlatformContentItem> items = List.of(new PlatformContentItem(
                platformContentId, "Idempotency test video", "desc", ContentType.VIDEO,
                Instant.now().minusSeconds(3600), 120, "en", "US", "Test", null));
        List<PlatformPerformanceSnapshot> performance = List.of(new PlatformPerformanceSnapshot(
                platformContentId, capturedAt, 1000, 50, 10, 5, null, null, null, Map.of("US", 1000L)));

        SyncContext ctx1 = accountLockService.claim(accountId);
        ingestionService.ingestAndComplete(ctx1, items, performance);

        SyncContext ctx2 = accountLockService.claim(accountId);
        ingestionService.ingestAndComplete(ctx2, items, performance);

        List<Content> matching = contentRepository.findBySocialAccountId(accountId).stream()
                .filter(c -> c.getPlatformContentId().equals(platformContentId))
                .toList();
        assertThat(matching).hasSize(1);

        UUID contentId = matching.get(0).getId();
        long snapshotCount = snapshotRepository.findByContentIdOrderByCapturedAtAsc(contentId).stream()
                .filter(s -> s.getCapturedAt().equals(capturedAt))
                .count();
        assertThat(snapshotCount).isEqualTo(1);
    }

    private UUID createConnectedAccount() {
        User user = userRepository.save(User.builder()
                .email("idempotency-test-" + UUID.randomUUID() + "@example.com")
                .passwordHash("irrelevant-for-this-test")
                .displayName("Idempotency Test")
                .build());
        SocialAccount account = socialAccountRepository.save(SocialAccount.builder()
                .user(user)
                .platform(Platform.MOCK)
                .platformAccountId("idempotency-test-" + UUID.randomUUID())
                .username("@idempotencytest")
                .connectedAt(Instant.now())
                .status(SocialAccountStatus.CONNECTED)
                .build());
        return account.getId();
    }
}
