# Database design

PostgreSQL, managed entirely through Flyway migrations in
`backend/src/main/resources/db/migration`. See [ADR-0002](adr/0002-postgresql.md)
for why Postgres specifically.

## Tables

| Table | Purpose |
|---|---|
| `users` | Account credentials (BCrypt hash only) and profile |
| `social_accounts` | One connected platform account per row; `(platform, platform_account_id)` unique |
| `content` | One row per published item; `(platform, platform_content_id)` unique - this is what makes ingestion idempotent |
| `content_snapshots` | **Append-only** historical performance observations; never updated |
| `content_region_metrics` | Latest known regional breakdown per content item (refreshed, not appended - platform APIs expose this as a lifetime aggregate, not a time series) |
| `topics` / `content_topics` | Normalized many-to-many topic tagging |
| `brands` / `content_brand_mentions` | Normalized many-to-many brand mention tracking |
| `sync_jobs` | One row per sync attempt: status, timing, attempt count, error message |
| `viral_events` | Persisted output of the viral-detection heuristic |

## Why historical snapshots, append-only

The entire "why did this go viral" and growth/acceleration analysis depends
on having the actual time series, not just the current totals. `content_snapshots`
is never `UPDATE`d after insert - every sync tick that observes new numbers
inserts a new row with its own `captured_at`. This is also what the growth
calculator (`GrowthCalculator`) operates on: `growth(t) = views(t) - views(t-1)`
over consecutive snapshot rows.

A unique constraint on `(content_id, captured_at)` prevents two inserts for
the same instant from silently duplicating a data point, while still allowing
many observations per day per content item.

## Idempotency at the schema level

```sql
CREATE UNIQUE INDEX ux_content_platform_content_id ON content (platform, platform_content_id);
```

The sync ingestion path (`ContentIngestionService`) always does
find-by-unique-key-then-upsert against this constraint. Running the same sync
twice (a legitimate retry, or an operator manually re-triggering it) updates
the same `content` row rather than creating a duplicate - see
[docs/transactions.md](transactions.md) for how this fits into the short
ingestion transaction.

## Identifiers

Every entity uses a randomly-generated `UUID` primary key
(`@GeneratedValue` with Hibernate's default UUID strategy), never a
sequential integer, so externally-exposed ids don't leak row counts or allow
enumeration.

## Optimistic locking

`BaseEntity` (the superclass for most entities) carries a `@Version` column.
This protects against lost updates on concurrently-edited rows (e.g. two
requests updating the same `social_accounts` row) with a cheap, no-lock-held
strategy appropriate for low-contention paths. It is **not** what prevents
duplicate concurrent syncs of the same account - that needs a stronger
guarantee than "the last writer wins, and we can tell after the fact." See
[docs/concurrency.md](concurrency.md) for why pessimistic locking is used
specifically for that path instead.

## Indexes

Indexes were added for the query patterns the application actually issues,
not defensively on every column:

- `content_snapshots (content_id, captured_at DESC)` - the timeline query.
- `content (platform, country)` - regional/platform analytics.
- `social_accounts (status)` and the `findDueForSync` query - the scheduler's
  "which accounts need a refresh" scan.
- `sync_jobs (social_account_id, created_at DESC)` - sync history lookups.

Every foreign key also has Postgres's automatic index on the referencing
column skipped only where the FK itself is the leading column of a more
specific composite index already listed above.
