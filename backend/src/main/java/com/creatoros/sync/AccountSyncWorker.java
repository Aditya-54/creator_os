package com.creatoros.sync;

import com.creatoros.social.integration.ConnectedAccount;
import com.creatoros.social.integration.PlatformContentItem;
import com.creatoros.social.integration.PlatformPerformanceSnapshot;
import com.creatoros.social.integration.SocialPlatformClient;
import com.creatoros.social.integration.SocialPlatformClientRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Runs exactly one account's sync cycle end to end: claim the lock, call the
 * external platform (outside any DB transaction), persist the results in a
 * short transaction, and record success/failure on the {@link SyncJob}. A
 * failure here is always isolated to this one account - see
 * {@link AccountSyncScheduler}, which submits one of these per due account
 * and never lets one exception abort the batch.
 */
@Component
public class AccountSyncWorker {

    private static final Logger log = LoggerFactory.getLogger(AccountSyncWorker.class);

    private final AccountLockService lockService;
    private final SocialPlatformClientRegistry clientRegistry;
    private final ContentIngestionService ingestionService;
    private final SyncJobService syncJobService;
    private final RetryPolicy retryPolicy;

    public AccountSyncWorker(AccountLockService lockService,
                              SocialPlatformClientRegistry clientRegistry,
                              ContentIngestionService ingestionService,
                              SyncJobService syncJobService,
                              RetryPolicy retryPolicy) {
        this.lockService = lockService;
        this.clientRegistry = clientRegistry;
        this.ingestionService = ingestionService;
        this.syncJobService = syncJobService;
        this.retryPolicy = retryPolicy;
    }

    public void syncAccount(java.util.UUID accountId) {
        SyncContext ctx;
        try {
            ctx = lockService.claim(accountId);
        } catch (SyncAlreadyInProgressException e) {
            log.info("Skipping sync for account {}: already in progress", accountId);
            return;
        }

        MDC.put("syncJobId", ctx.syncJobId().toString());
        MDC.put("socialAccountId", ctx.accountId().toString());
        MDC.put("platform", ctx.platform().name());
        try {
            SocialPlatformClient client = clientRegistry.resolve(ctx.platform());
            ConnectedAccount connectedAccount = new ConnectedAccount(ctx.accountId(), ctx.platformAccountId(), ctx.username());

            retryPolicy.executeWithRetry(
                    () -> runOnce(client, connectedAccount, ctx),
                    attempt -> {
                        log.warn("Sync attempt {} failed for account {}, will retry", attempt, accountId);
                        syncJobService.markRetrying(ctx.syncJobId(), attempt + 1, "Retrying after attempt " + attempt + " failed");
                    });
            log.info("Sync succeeded for account {}", accountId);
        } catch (Exception e) {
            log.error("Sync permanently failed for account {}", accountId, e);
            syncJobService.markFailed(ctx.syncJobId(), ctx.accountId(), e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    private Void runOnce(SocialPlatformClient client, ConnectedAccount account, SyncContext ctx) {
        List<PlatformContentItem> items = client.fetchContent(account);
        List<String> ids = items.stream().map(PlatformContentItem::platformContentId).toList();
        List<PlatformPerformanceSnapshot> performance = client.fetchPerformance(account, ids);
        ingestionService.ingestAndComplete(ctx, items, performance);
        return null;
    }
}
