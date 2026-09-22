package com.creatoros.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.creatoros.social.Platform;
import com.creatoros.social.SocialAccount;
import com.creatoros.social.SocialAccountRepository;
import com.creatoros.social.SocialAccountStatus;
import com.creatoros.support.AbstractIntegrationTest;
import com.creatoros.user.User;
import com.creatoros.user.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proves the account-level locking claim in docs/concurrency.md: when two
 * workers race to claim the same social account's sync, exactly one succeeds
 * and the other is rejected with {@link SyncAlreadyInProgressException} -
 * never both, and never a duplicate {@code RUNNING} sync_jobs row. Requires a
 * real PostgreSQL instance (Testcontainers/Docker) because the guarantee
 * being tested (`SELECT ... FOR UPDATE`) depends on real row-level locking,
 * not an in-memory approximation.
 */
class AccountLockServiceConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private AccountLockService accountLockService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SocialAccountRepository socialAccountRepository;
    @Autowired
    private SyncJobRepository syncJobRepository;

    @Test
    void onlyOneOfTwoConcurrentClaimsSucceeds() throws InterruptedException {
        UUID accountId = createConnectedAccount();

        int workerCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(workerCount);
        CountDownLatch readyLatch = new CountDownLatch(workerCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(workerCount);
        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < workerCount; i++) {
            pool.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    accountLockService.claim(accountId);
                    results.add(true);
                } catch (SyncAlreadyInProgressException e) {
                    results.add(false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        long successCount = results.stream().filter(Boolean::booleanValue).count();
        assertThat(successCount).isEqualTo(1);
        assertThat(results).hasSize(workerCount);

        List<SyncJob> jobs = syncJobRepository.findRecentForAccount(accountId, org.springframework.data.domain.PageRequest.of(0, 20));
        long runningJobs = jobs.stream().filter(j -> j.getStatus() == SyncStatus.RUNNING).count();
        assertThat(runningJobs).isEqualTo(1);
    }

    private UUID createConnectedAccount() {
        User user = userRepository.save(User.builder()
                .email("lock-test-" + UUID.randomUUID() + "@example.com")
                .passwordHash("irrelevant-for-this-test")
                .displayName("Lock Test")
                .build());
        SocialAccount account = socialAccountRepository.save(SocialAccount.builder()
                .user(user)
                .platform(Platform.MOCK)
                .platformAccountId("lock-test-" + UUID.randomUUID())
                .username("@locktest")
                .connectedAt(Instant.now())
                .status(SocialAccountStatus.CONNECTED)
                .build());
        return account.getId();
    }
}
