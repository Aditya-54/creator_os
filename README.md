# CreatorOS

**AI-powered cross-platform content intelligence platform.**

CreatorOS is not another platform-analytics clone. It normalizes performance
data from multiple social platforms into one historical store, then builds an
analytical reasoning layer on top: growth/acceleration math, deterministic
viral-event detection, "explain this spike" evidence gathering, and an
evidence-grounded AI analyst that can only answer using real stored data (it
never queries the database directly - see [docs/ai-and-mcp.md](docs/ai-and-mcp.md)).

## Problem

Creators publish the same idea across YouTube, Instagram, and other
platforms. Each platform's own analytics dashboard only shows that platform in
isolation, in that platform's own vocabulary. Answering a question like *"why
did my football content do better on YouTube than Instagram, and does that
hold for gaming content too?"* means manually exporting numbers from multiple
dashboards and reasoning about them by hand - platform APIs don't do
cross-platform, cross-topic, historical reasoning for you.

## Solution

CreatorOS ingests content and performance history from each connected
platform through a common adapter interface, stores it as an append-only time
series, and runs the same analytics engine over all of it regardless of
platform. On top of that: a deterministic viral-event detector, an
evidence-first "explain this spike" feature, and an AI analyst that is only
allowed to answer using tool calls into that same analytics engine.

## Engineering highlights

This is primarily a backend/Spring Boot showcase. See
[docs/demo-script.md](docs/demo-script.md) for a guided walkthrough and
"Interesting engineering problems" below for the parts worth discussing in an
interview.

- **Spring Boot 3.4 + Java 21**, layered by feature (`user`, `social`, `content`,
  `topic`, `brand`, `sync`, `analytics`, `ai`) rather than by technical layer.
- **JWT authentication** (stateless, HS256, BCrypt password hashing) with a
  `@RestControllerAdvice`-based uniform error envelope.
- **PostgreSQL + Flyway** with normalized tables, unique constraints doing the
  work of idempotency, and append-only historical snapshots.
- **Account-level concurrency control**: `SELECT ... FOR UPDATE` makes
  "is a sync already running for this account?" atomic across concurrent
  transactions/instances - see [docs/concurrency.md](docs/concurrency.md).
- **Bounded thread pool** (`ThreadPoolTaskExecutor`, all sizes configurable)
  for concurrent account synchronization, with a `CallerRunsPolicy` backpressure
  strategy instead of unbounded queueing.
- **Short, deliberately-scoped transactions**: external API calls always
  happen outside any `@Transactional` boundary - see
  [docs/transactions.md](docs/transactions.md).
- **Idempotent ingestion** via `(platform, platform_content_id)` unique
  constraints and upsert-by-lookup, so re-running a sync never duplicates data.
- **Bounded retry with exponential backoff** for transient sync failures,
  tracked per-attempt on a `sync_jobs` row.
- **Deterministic, explainable viral detection** - a heuristic over the
  content's own growth history, not a black box, and never phrased as proof of
  causation.
- **Evidence-grounded AI** via Spring AI: the model can only answer by calling
  typed tool methods backed by the same analytics services the REST API uses.
  The whole application - including the AI analyst falling back to a plain
  overview - keeps working with zero AI provider configured.
- **MCP server** exposing the same analytics tools to any MCP-compatible
  client (e.g. Claude Desktop).
- Real integration boundary for **YouTube** (Data API v3, public read-only via
  API key), and a clearly-labeled **deterministic demo/mock** boundary for
  Instagram and the `MOCK` platform, behind one `SocialPlatformClient`
  interface - see [docs/architecture.md](docs/architecture.md).

## Interesting engineering problems

- **Preventing duplicate concurrent syncs** of the same social account across
  workers/instances without holding a lock during a slow external API call
  ([docs/concurrency.md](docs/concurrency.md)).
- **Keeping transactions short**: claim the sync (fast DB transaction) →
  call the external API (no transaction) → persist results (fast DB
  transaction), rather than one long transaction spanning the network call
  ([docs/transactions.md](docs/transactions.md)).
- **Making ingestion idempotent** so re-syncing (or retrying after a failure)
  never creates duplicate content or snapshot rows.
- **Bounding retries** for rate limits/timeouts without retrying permanent
  auth failures forever.
- **Preventing LLM hallucination through evidence-grounded tools**: the AI
  analyst is architecturally incapable of inventing a metric, because it can
  only "see" numbers that come back from a tool call into tested services.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA, Flyway, springdoc-openapi |
| AI / MCP | Spring AI 1.0 (OpenAI + Anthropic model starters, MCP server) |
| Database | PostgreSQL 16 |
| Frontend | React 19, TypeScript, Vite, Recharts |
| Infra | Docker, Docker Compose |
| Testing | JUnit 5, Mockito, Testcontainers |

## Running locally

### Option A: Docker Compose (recommended)

```bash
cp .env.example .env
# edit .env - at minimum set JWT_SECRET and TOKEN_ENCRYPTION_KEY to random strings
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator health: http://localhost:8080/actuator/health

No AI provider or social platform credentials are required to explore the
product - log in, then click **"Load demo workspace"** on the dashboard to
seed a rich, clearly-labeled demo dataset (see
[docs/demo-script.md](docs/demo-script.md)).

### Option B: Run backend and frontend directly

Requires a local PostgreSQL instance (or `docker compose up postgres`).

```bash
# Backend
cd backend
cp ../.env.example .env   # or export the same variables another way
./mvnw spring-boot:run

# Frontend (separate terminal)
cd frontend
npm install
npm run dev
```

## Environment variables

See [.env.example](.env.example) for the full list with descriptions. Only
`JWT_SECRET` and `TOKEN_ENCRYPTION_KEY` need real values to run the app -
everything else has a safe local-dev default, and AI/YouTube credentials are
entirely optional.

## Tests

```bash
cd backend
./mvnw test                 # unit tests (no external dependencies)
./mvnw verify                # includes Testcontainers-backed integration tests (requires Docker)
```

```bash
cd frontend
npm run build                # type-checks and builds
```

## Documentation

- [docs/architecture.md](docs/architecture.md) - module layout and key design decisions
- [docs/database.md](docs/database.md) - schema and indexing rationale
- [docs/concurrency.md](docs/concurrency.md) - account-level locking and the bounded executor
- [docs/transactions.md](docs/transactions.md) - why external calls stay outside `@Transactional`
- [docs/security.md](docs/security.md) - auth, token encryption, CORS
- [docs/ai-and-mcp.md](docs/ai-and-mcp.md) - AI provider abstraction, tool-grounding, MCP server
- [docs/api.md](docs/api.md) - REST API overview (full detail in Swagger UI)
- [docs/demo-script.md](docs/demo-script.md) - a guided walkthrough for a demo/interview
- [docs/adr/](docs/adr/) - architecture decision records

## Known limitations

- The YouTube integration uses API-key-only public reads (Data API v3); share
  count, watch time, average view duration, and regional breakdowns require
  the YouTube Analytics API and OAuth authorization from the channel owner,
  which is out of scope for a student/portfolio deployment. Those fields are
  left `null`/`0` for real YouTube accounts rather than fabricated - see
  [docs/architecture.md](docs/architecture.md).
- Instagram has no working real integration: the Graph API requires Meta App
  Review and a connected Business/Creator account, which isn't obtainable for
  this project. It is implemented as a clearly-labeled deterministic mock
  behind the same adapter interface a real implementation would use.
- The standalone MCP server has no authentication of its own (see
  [docs/ai-and-mcp.md](docs/ai-and-mcp.md)) - it's intended for local/trusted
  use (e.g. pointing Claude Desktop at `localhost` during a demo), not for
  exposing on a public network as-is.
