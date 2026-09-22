package com.creatoros.sync;

import com.creatoros.exception.ResourceNotFoundException;
import com.creatoros.social.SocialAccount;
import com.creatoros.social.SocialAccountRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarantees only one worker ever synchronizes a given social account at a
 * time, even across multiple application instances.
 *
 * <p><b>Why pessimistic row locking instead of {@code synchronized} or
 * optimistic locking:</b> a plain Java lock only works within a single JVM,
 * which breaks the moment CreatorOS runs as more than one instance.
 * Optimistic locking (the {@code version} column) would let two transactions
 * both read "no job running", both decide to proceed, and only fail one of
 * them *after* wasting a full external API round trip. A
 * {@code SELECT ... FOR UPDATE} on the {@code social_accounts} row makes the
 * "is a job already running for this account?" check-then-insert atomic
 * across transactions: the second worker blocks on the row lock until the
 * first transaction commits, then immediately sees the just-inserted
 * {@code RUNNING} job and backs off - before either of them has made an
 * external call. See docs/concurrency.md.
 */
@Service
public class AccountLockService {

    private static final Set<SyncStatus> IN_PROGRESS_STATUSES = EnumSet.of(SyncStatus.RUNNING, SyncStatus.RETRYING);

    private final SocialAccountRepository socialAccountRepository;
    private final SyncJobRepository syncJobRepository;

    public AccountLockService(SocialAccountRepository socialAccountRepository, SyncJobRepository syncJobRepository) {
        this.socialAccountRepository = socialAccountRepository;
        this.syncJobRepository = syncJobRepository;
    }

    /**
     * Claims the right to sync {@code accountId}. Deliberately short: only a
     * row lock, an existence check, and one insert - no external I/O happens
     * inside this transaction (docs/transactions.md).
     */
    @Transactional
    public SyncContext claim(UUID accountId) {
        SocialAccount account = socialAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("SocialAccount", accountId));

        if (syncJobRepository.existsBySocialAccount_IdAndStatusIn(accountId, IN_PROGRESS_STATUSES)) {
            throw new SyncAlreadyInProgressException(accountId);
        }

        SyncJob job = syncJobRepository.save(SyncJob.builder()
                .socialAccount(account)
                .status(SyncStatus.RUNNING)
                .startedAt(Instant.now())
                .attemptCount(1)
                .build());

        return new SyncContext(job.getId(), account.getId(), account.getPlatform(),
                account.getPlatformAccountId(), account.getUsername());
    }
}
