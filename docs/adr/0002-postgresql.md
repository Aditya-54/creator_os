# ADR-0002: PostgreSQL

## Status

Accepted

## Decision

Use PostgreSQL as the sole datastore.

## Rationale

- The core domain is relational: users own social accounts, which own
  content, which owns a time series of snapshots, joined against topics and
  brands. Foreign keys and unique constraints (see docs/database.md) are not
  incidental here - the `(platform, platform_content_id)` unique constraint is
  what makes ingestion idempotent, and that guarantee is much weaker in a
  schemaless store.
- `SELECT ... FOR UPDATE` (see docs/concurrency.md) is a first-class,
  well-understood mechanism in Postgres for the account-locking requirement,
  and it works correctly across multiple application instances since the lock
  lives in the database, not in any one process.
- Postgres is free to run locally via Docker with zero licensing concerns,
  satisfying the "no mandatory paid infrastructure" requirement.
- Analytics queries here are mostly per-user aggregations over a few thousand
  rows at most (a single creator's content history) - well within what a
  relational database handles comfortably without needing a specialized
  analytics/OLAP store.

## Alternatives considered

- **MongoDB**: would make the unique-constraint-based idempotency and the
  row-locking-based concurrency control both harder to express correctly,
  for a workload that is naturally relational anyway. Rejected.
- **Redis (as a queue/lock)**: would add an extra piece of infrastructure for
  a locking need Postgres already satisfies for free. Rejected per the
  project's "don't overengineer" constraint.

## Consequences

- Flyway migrations are the single source of truth for schema; there is no
  runtime schema flexibility, which is the correct tradeoff for a domain this
  structured.
