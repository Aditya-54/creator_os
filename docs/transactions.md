# Transactions

## The rule: never hold a database transaction open across a slow external call

```text
Worker
 |
 +-- call external social API        <-- no @Transactional here
 |
 +-- normalize result                <-- plain Java, no @Transactional here
 |
 +-- short DB transaction
      |
      +-- upsert content
      +-- insert snapshot(s)
      +-- upsert region metrics
      +-- update social_account.last_synced_at/status
      +-- mark sync_job SUCCESS
      |
      COMMIT
```

`AccountSyncWorker.syncAccount` follows this shape literally:

1. `AccountLockService.claim(accountId)` - its own short `@Transactional` method.
2. `client.fetchContent(...)` / `client.fetchPerformance(...)` - plain method
   calls to a `SocialPlatformClient`, no transaction, no repository access at all.
3. `ContentIngestionService.ingestAndComplete(...)` - one short
   `@Transactional` method that does every database write for this sync cycle.

A Postgres connection held open (and a row/table potentially locked, depending
on what else the transaction touched) for the duration of an HTTP round trip
to YouTube or Instagram would:

- hold a connection out of the pool for however long that external call takes
  (which, under rate limiting or a slow network, could be seconds),
- under load, exhaust the connection pool with connections that are doing
  nothing but waiting on a socket read to a third party,
- and if that external call also happens to be blocked behind a lock this
  transaction is holding, create a completely unnecessary contention point.

Keeping the transaction boundary around *only* the database work means a slow
or failing external API affects that one sync attempt's latency, never the
database's capacity to serve other requests.

## Where `@Transactional` is actually used

| Method | Scope | Why |
|---|---|---|
| `AccountLockService.claim` | row lock + existence check + insert | Must be atomic to prevent duplicate concurrent syncs - see [docs/concurrency.md](concurrency.md) |
| `ContentIngestionService.ingestAndComplete` | upsert content/snapshots/regions + update account + update sync job | All-or-nothing: a partial ingest (content written, snapshot not) would be a worse failure mode than the whole cycle rolling back |
| `SyncJobService.markRetrying` / `markFailed` | one row update | Deliberately its own transaction, separate from the retry loop's external call |
| `AuthService.register` / `login` | user lookup/insert + password check | Standard request-scoped unit of work |
| Analytics read services | `readOnly = true` | Signals to Hibernate/the driver that no flush is needed, and documents intent |

## Read-only transactions for analytics

Every analytics query method is `@Transactional(readOnly = true)`. This isn't
just documentation - it lets Hibernate skip dirty-checking on the loaded
entities and lets the JDBC driver apply any read-only optimizations it
supports, which matters because analytics endpoints are the highest-read-volume
part of the API.

## `open-in-view` is disabled

`spring.jpa.open-in-view: false` in `application.yml`. The Open Session In
View pattern would let a controller lazily trigger additional queries after
the service-layer transaction has already committed, which hides N+1 query
bugs and makes it unclear where a given query actually executes. Every lazy
association needed by a DTO is loaded inside the owning service's transaction
instead.
