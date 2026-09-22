# API overview

Full request/response schemas, validation rules, and example payloads are
generated from the live code at `/swagger-ui.html` (OpenAPI JSON at
`/v3/api-docs`). This page is a map, not the full reference.

All endpoints except those listed under **Public** require
`Authorization: Bearer <token>`.

## Auth (public)

| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/register` | email + password (min 8 chars) + optional display name |
| POST | `/api/auth/login` | returns `AuthResponse` with a bearer token |
| GET | `/api/auth/me` | current user (protected) |

## Social accounts

| Method | Path | Notes |
|---|---|---|
| GET | `/api/social-accounts` | accounts owned by the current user |
| POST | `/api/social-accounts` | `{ platform, handle }` - connects via the matching `SocialPlatformClient` |
| DELETE | `/api/social-accounts/{id}` | disconnects; cascades to that account's content |
| POST | `/api/social-accounts/{id}/sync` | submits an async sync; returns `202 Accepted` immediately |

## Content

| Method | Path | Notes |
|---|---|---|
| GET | `/api/content` | paginated, newest published first |
| GET | `/api/content/{id}` | single item with resolved topics |
| GET | `/api/content/{id}/snapshots` | full historical snapshot series |

## Analytics

| Method | Path | Notes |
|---|---|---|
| GET | `/api/analytics/overview` | cross-platform totals, top content, recent viral events |
| GET | `/api/analytics/content/{id}` | current totals + latest growth/acceleration |
| GET | `/api/analytics/content/{id}/timeline` | full `GrowthPoint` series |
| GET | `/api/analytics/content/{id}/viral-events` | detects (and persists) + returns viral episodes |
| GET | `/api/analytics/content/{id}/explanation` | "explain this spike" - evidence, factors, comparisons, limitations |
| GET | `/api/analytics/topics` | performance aggregated by topic |
| GET | `/api/analytics/regions` | performance aggregated by country |
| GET | `/api/analytics/platforms` | performance aggregated by platform |

## AI

| Method | Path | Notes |
|---|---|---|
| POST | `/api/ai/query` | `{ question }` -> evidence-grounded answer, or a graceful fallback if no AI provider is configured |

## Demo data

| Method | Path | Notes |
|---|---|---|
| POST | `/api/demo/seed` | populates the current user's workspace with ~14 clearly-labeled demo content items and rich history |
| DELETE | `/api/demo/reset` | removes only the demo accounts/content for the current user |

## Error shape

Every error response, regardless of cause, has this shape:

```json
{
  "timestamp": "2026-09-22T10:15:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/auth/register",
  "fieldErrors": [{ "field": "email", "message": "must be a well-formed email address" }]
}
```

`code` is stable and meant to be branched on by clients; `message` is
human-readable and may change wording over time.
