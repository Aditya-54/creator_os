package com.creatoros.sync;

import com.creatoros.social.SocialAccount;
import com.creatoros.social.SocialAccountRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.core.task.TaskExecutor;

/**
 * Periodically finds accounts due for a refresh and submits one
 * {@link AccountSyncWorker} task per account to the bounded
 * {@code syncTaskExecutor}. Submission is fire-and-forget: each task manages
 * its own locking, retries, and failure handling, so a slow or failing
 * account never blocks the scheduler from moving on to the next one.
 */
@Component
public class AccountSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccountSyncScheduler.class);

    private final SocialAccountRepository socialAccountRepository;
    private final AccountSyncWorker worker;
    private final TaskExecutor syncTaskExecutor;
    private final SyncProperties properties;

    public AccountSyncScheduler(SocialAccountRepository socialAccountRepository,
                                 AccountSyncWorker worker,
                                 @Qualifier("syncTaskExecutor") TaskExecutor syncTaskExecutor,
                                 SyncProperties properties) {
        this.socialAccountRepository = socialAccountRepository;
        this.worker = worker;
        this.syncTaskExecutor = syncTaskExecutor;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${creatoros.sync.interval-ms}")
    public void syncDueAccounts() {
        Instant staleBefore = Instant.now().minus(properties.staleAfterMinutes(), ChronoUnit.MINUTES);
        List<SocialAccount> due = socialAccountRepository.findDueForSync(staleBefore);
        if (due.isEmpty()) {
            return;
        }
        log.info("Scheduler found {} account(s) due for sync", due.size());
        for (SocialAccount account : due) {
            var accountId = account.getId();
            syncTaskExecutor.execute(() -> worker.syncAccount(accountId));
        }
    }
}
