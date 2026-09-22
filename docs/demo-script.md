# Demo script

A guided walkthrough for showing CreatorOS in an interview or portfolio
review. Assumes `docker compose up --build` is running (see the root
[README.md](../README.md)).

## 1. Register and log in

Open http://localhost:5173, create an account, and you'll land on an empty
dashboard - no fake data appears until you ask for it.

## 2. Load the demo workspace

Click **"Load demo workspace"**. This calls `POST /api/demo/seed`, which
creates two demo social accounts (`YOUTUBE`, `INSTAGRAM`) and ~14 hand-authored
content items with full historical snapshot series - everything is tagged
`is_demo: true` and visibly labeled `DEMO` in the UI. This bypasses the sync
pipeline (it's a bulk seed, not a live sync) but writes exactly the same
shape of data a real sync would produce.

## 3. Tour the cross-platform dashboard

Point out: total views/engagement across both platforms in one place, the
views-by-platform chart, top content ranked by views, and recent viral events
- all computed from the same `content_snapshots` history regardless of which
platform the content came from.

## 4. Open a viral content item

Go to Content → pick **"Last-minute winner in the derby!"**. Its growth
timeline shows a flat period followed by a sharp, delayed acceleration
(demo example: delayed viral growth several hours after publishing) - point
out the `ReferenceLine` marker where the detector flagged it.

## 5. Inspect the timeline, then click "Explain this spike"

The viral-events panel shows the detector's own output: multiplier,
confidence label, and a plain-language explanation - all deterministic, no AI
involved. Then click **"Explain this spike"**
(`GET /api/analytics/content/{id}/explanation`) to show the fuller
evidence-gathering pass: growth evidence, regional concentration, a
comparison against similar same-topic content, and - importantly - an
explicit **limitations** section that says the data can't prove causation.

## 6. Ask the AI analyst a grounded question

Go to AI Analyst and ask:

> "Why did my football content outperform my gaming content?"

If an AI provider is configured (`AI_PROVIDER` + an API key in `.env`), watch
the answer cite real numbers - this is Spring AI calling into
`AnalyticsTools`, which calls the same `TopicAnalyticsService` the REST
`/api/analytics/topics` endpoint uses. If no provider is configured, the
response explains that plainly and falls back to the content overview instead
of erroring - demonstrating the "AI is fully optional" requirement live.

Other good prompts: *"Which topics work best in India?"*, *"What changed when
my growth accelerated?"*, *"What should I test next based on my historical
data?"*

## 7. Show regional analysis

`GET /api/analytics/regions` (or ask the AI analyst about India specifically)
- the demo dataset deliberately skews football content toward India, so this
should surface that pattern with real numbers behind it, not a guess.

## 8. Show the MCP tools

If you have Claude Desktop or another MCP client available, point it at
`http://localhost:8080/sse` and list the available tools
(`get_growth_timeline`, `get_viral_events`, `compare_platforms`, etc.) - the
same tools the in-app AI analyst used in step 6, now callable from any
MCP-compatible client. See [docs/ai-and-mcp.md](ai-and-mcp.md) for the exact
list and the trust tradeoff of this server having no auth of its own.

## Things worth calling out if asked

- Every number on screen traces back to a real row in `content_snapshots` -
  nothing is computed client-side.
- The viral detector and "explain this spike" both work with zero AI
  configuration; the AI analyst adds natural-language synthesis on top of the
  same evidence, it doesn't replace it.
- Reconnecting/re-syncing an account is safe to run twice - see
  [docs/database.md](database.md) and [docs/concurrency.md](concurrency.md).
