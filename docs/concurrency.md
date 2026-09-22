# Concurrency

Two distinct concurrency concerns are handled here, deliberately with
different tools.

## 1. Bounded parallelism across accounts

`AccountSyncScheduler` runs on a `@Scheduled` fixed delay
(`creatoros.sync.interval-ms`), finds accounts due for a refresh
(`findDueForSync`), and submits one `AccountSyncWorker.syncAccount(accountId)`
task per account to a dedicated `ThreadPoolTaskExecutor`:

```java
@Bean(name = "syncTaskExecutor")
public ThreadPoolTaskExecutor syncTaskExecutor(SyncProperties properties) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.executor().corePoolSize());
    executor.setMaxPoolSize(properties.executor().maxPoolSize());
    executor.setQueueCapacity(properties.executor().queueCapacity());
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    ...
}
```

All three sizes come from configuration
(`creatoros.sync.executor.core-pool-size` / `max-pool-size` / `queue-capacity`),
never hardcoded. `CallerRunsPolicy` was chosen over dropping/discarding tasks
when the queue is full: it applies backpressure to the scheduler thread
instead of silently losing a sync request, which matters more than raw
throughput for a job that runs every 15 minutes by default.

A failure inside one `AccountSyncWorker.syncAccount` call is caught inside
that method and recorded on its own `sync_jobs` row - it never propagates
back to the scheduler, so one broken account can never take down
synchronization for every other account (section 27 of the product spec).

## 2. Preventing duplicate concurrent syncs of the *same* account

This is the part worth discussing in an interview. Two workers (or, in a
multi-instance deployment, two application instances) could both decide
account `#17` is due for a sync at the same moment.

**Why not a Java `synchronized` block or an in-memory lock map?** It only
works within one JVM. The moment CreatorOS runs as more than one instance -
which is a completely ordinary horizontal-scaling step - an in-memory lock
stops preventing anything, silently.

**Why not optimistic locking (the `version` column)?** Optimistic locking
would let both transactions read "no job currently running for this
account", both decide to proceed, and only fail one of them *after* it has
already made a slow external API call and tried to commit. That's wasted
work and a confusing failure mode, not real prevention.

**What CreatorOS does instead:** `AccountLockService.claim()` takes a
pessimistic row lock on `social_accounts`, checks whether a `sync_jobs` row
for that account is already `RUNNING`/`RETRYING`, and if not, inserts a new
`RUNNING` job - all inside one short transaction:

```java
@Transactional
public SyncContext claim(UUID accountId) {
    SocialAccount account = socialAccountRepository.findByIdForUpdate(accountId) // SELECT ... FOR UPDATE
            .orElseThrow(...);
    if (syncJobRepository.existsBySocialAccount_IdAndStatusIn(accountId, IN_PROGRESS_STATUSES)) {
        throw new SyncAlreadyInProgressException(accountId);
    }
    SyncJob job = syncJobRepository.save(SyncJob.builder()...status(RUNNING)...build());
    return new SyncContext(job.getId(), ...);
}
```

`findByIdForUpdate` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`, which
Hibernate translates to `SELECT ... FOR UPDATE`. The second worker's call to
`claim()` blocks on that row lock until the first transaction commits (or
rolls back). Once it does, the second worker's own `existsBySocialAccount_IdAndStatusIn`
check sees the just-committed `RUNNING` row and backs off with
`SyncAlreadyInProgressException` - correctly, and without ever making a
redundant external API call.

This works across multiple application instances too, because the lock lives
in PostgreSQL, not in process memory.

## Why the lock is held for milliseconds, not the whole sync

`claim()` does nothing except the row lock, one existence check, and one
insert - no HTTP call, no JSON parsing. The lock is released the instant that
transaction commits. Everything slow (calling the platform API) happens
*after* `claim()` returns, with no open transaction at all - see
[docs/transactions.md](transactions.md) for why that separation matters on
its own.

## Retry and backoff

`RetryPolicy.executeWithRetry` wraps the external-call-plus-ingestion step
with bounded exponential backoff (`creatoros.sync.retry.*`). It runs on the
sync executor's own worker thread, so a blocking `Thread.sleep` between
attempts never ties up an HTTP request thread. On each failed attempt before
the last, `SyncJobService.markRetrying` records the attempt count and error in
a short transaction, so `sync_jobs` shows real attempt history even while a
retry sequence is still in progress. Permanent failures (e.g. a missing API
key) still exhaust their configured attempts rather than looping forever -
this project does not attempt to classify transient vs. permanent failures
beyond that bound, which is a deliberate scope simplification for a portfolio
project (see the retry loop in `AccountSyncWorker` for where a real
classification would slot in).
