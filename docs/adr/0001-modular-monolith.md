# ADR-0001: Modular monolith, not microservices

## Status

Accepted

## Context

CreatorOS has several distinct concerns (auth, social integration, content
ingestion, analytics, AI). Microservices would be one way to isolate them.

## Decision

Build one Spring Boot application, organized into feature packages
(`user`, `social`, `content`, `sync`, `analytics`, `ai`, ...), backed by one
PostgreSQL database.

## Rationale

- The concerns here are tightly coupled by data: analytics reads content and
  snapshots written by sync; the AI layer reads analytics. Splitting these
  into services would mean either a lot of network calls for what are
  currently in-process method calls, or duplicating data across service
  boundaries - neither buys anything at this scale.
- A single deployable and a single database massively simplify local
  development and the Docker Compose setup that this project's "runnable with
  zero paid infrastructure" requirement depends on.
- Package-per-feature already gives the organizational benefit people
  usually reach for microservices for (clear ownership boundaries, no
  circular dependencies) without the operational cost (service discovery,
  distributed transactions, network failure handling between your own
  services).
- Nothing about this design blocks extracting a service later if a real
  scaling need appears - the feature packages already have clean boundaries.

## Consequences

- Simpler to reason about, test, and deploy.
- All feature packages currently share one JVM's resources; a hot path in one
  feature (e.g. the sync executor) could theoretically starve another. In
  practice the bounded thread pool (see docs/concurrency.md) makes this an
  explicit, tunable limit rather than an emergent problem.
