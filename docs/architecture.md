# Architecture

## Modular monolith, not microservices

```text
React Frontend
      |
      | REST / JWT
      v
Spring Boot Application
      |
      +-- security      (JWT issuing/validation, password hashing, CORS)
      +-- user           (registration, login, account)
      +-- social          (connected accounts, platform adapters)
      +-- content          (ingested content + historical snapshots)
      +-- topic / brand      (rule-based classification)
      +-- sync                (locking, executor, retry, ingestion, scheduler)
      +-- analytics             (growth math, viral detection, explain-spike)
      +-- ai                     (provider abstraction, MCP tools)
      +-- demo                    (deterministic demo dataset)
      |
      v
PostgreSQL
```

Everything above runs as one Spring Boot process. There is one database, one
deployable artifact, and no message broker. This is a deliberate choice, not
a shortcut - see [ADR-0001](adr/0001-modular-monolith.md).

Packages are organized **by feature**, not by technical layer
(`controller`/`service`/`repository` folders per feature, not one giant
`controllers` package for the whole app). Each feature package still follows
the same internal shape: `Controller` (thin) → `Service` (business logic) →
`Repository` (persistence) → `Entity`, with DTOs at the boundary so JPA
entities are never serialized directly to clients.

## The platform adapter boundary

```java
public interface SocialPlatformClient {
    Platform platform();
    PlatformProfile fetchAccountProfile(SocialConnectRequest request);
    List<PlatformContentItem> fetchContent(ConnectedAccount account);
    List<PlatformPerformanceSnapshot> fetchPerformance(ConnectedAccount account, List<String> platformContentIds);
}
```

Nothing above the adapter layer - sync orchestration, ingestion, analytics -
knows which platform it's talking to beyond the `Platform` enum tag on the
data. Three implementations exist:

- **`YouTubeSocialPlatformClient`** - a real integration against the public
  YouTube Data API v3 using an API key (no OAuth). Channel lookup, video
  metadata, and public statistics (views/likes/comments) are genuinely fetched
  from Google's API. Share count, watch time, average view duration, and
  regional breakdowns are only available via the YouTube Analytics API, which
  requires OAuth authorization from the channel owner - those fields are left
  `null`/`0` here rather than invented. `YouTubeProperties.clientId/clientSecret`
  are reserved for wiring that OAuth flow up later; the adapter boundary
  already supports it without any other code changing.
- **`InstagramSocialPlatformClient`** - a **demo/mock** implementation, clearly
  documented as such in its Javadoc and in every piece of content it returns
  (titles are prefixed `[DEMO]`, `content.is_demo = true`). The Instagram
  Graph API requires Meta App Review and a Business/Creator account owned by
  the app before it returns real insights, which is not obtainable for a
  student portfolio project. Faking a "real" integration behind a fabricated
  OAuth flow would violate the product's own rule against presenting
  synthetic data as real, so this adapter is honest about what it is instead.
  Swapping in the real Graph API later only means replacing this one
  `@Component` - see its Javadoc for the exact endpoints to call.
- **`MockSocialPlatformClient`** - the `MOCK` platform, a fully synthetic
  account a user can connect to see the sync pipeline work end-to-end without
  any credentials at all.

Both mock adapters and the demo data generator (`DemoDataGenerator`) share
`ContentGrowthModel`, a small deterministic logistic-growth function seeded
from a stable string key, so re-running a sync against a mock account
produces a curve that agrees with itself over time instead of random noise
each call.

## Why mock unsupported social APIs?

Because the alternative is worse: either scrape platforms that forbid it, or
pretend a fabricated response came from a real API call. Both would violate
the product's core promise (evidence-grounded, not fabricated). Mocking
behind the *same interface* a real adapter would implement means: (a) it's
always obvious in the data (`is_demo`, `[DEMO]` titles, Javadoc) that this
isn't live data, and (b) the moment real API access is available, the fix is
a new class, not a rewrite of the sync/analytics layers.

## Why keep MCP tools as the only AI-to-data path?

See [docs/ai-and-mcp.md](ai-and-mcp.md) - the short version is that an LLM
with direct database access can construct a plausible-sounding but wrong
query; a fixed set of typed tool methods, each backed by a tested analytics
service, bounds what the model can possibly claim to whatever those services
actually computed.

## Rule-based topic and brand extraction

`TopicExtractionService` and `BrandExtractionService` do whole-word keyword
matching against title/description text (see their Javadoc for why "whole
word" matters - naive substring matching on a keyword like `ai` false-positives
on `training`). This runs during ingestion with zero AI configuration
required, per section 12 of the product spec: topic assignment cannot depend
on an LLM being configured. `TopicSource.AI` exists in the schema so an
LLM-assisted classifier could be added later as an additional source, without
changing how `content_topics` is queried.
