# Security

## Authentication

- Passwords are hashed with `BCryptPasswordEncoder` (`SecurityConfig`); the
  raw password never touches storage or logs.
- Login/registration return a signed JWT (`JwtService`, HS256 via `jjwt`).
  The signing key comes from `creatoros.jwt.secret` and must be at least 32
  bytes - `JwtService`'s constructor throws `IllegalStateException` immediately
  at startup if it isn't, rather than failing confusingly on the first login.
- `JwtAuthenticationFilter` runs once per request, parses a
  `Authorization: Bearer <token>` header if present, and populates the
  security context with an `AuthenticatedUser(userId, email)` principal. An
  invalid/expired token is treated as "no authentication" (not a hard 401 at
  the filter level) so that public endpoints stay reachable; `SecurityConfig`
  decides which paths actually require authentication.
- Protected endpoints resolve the current user via
  `@AuthenticationPrincipal AuthenticatedUser principal` - controllers never
  trust a client-supplied user id.

## What's public vs protected

`SecurityConfig.PUBLIC_ENDPOINTS`: registration, login, Actuator
health/info, Swagger UI/OpenAPI JSON, and the standalone MCP server endpoints
(`/sse/**`, `/mcp/**` - see [docs/ai-and-mcp.md](ai-and-mcp.md) for why those
specifically can't require a JWT). Everything else requires authentication by
default (`anyRequest().authenticated()`).

## Social account tokens at rest

`social_accounts.access_token_encrypted` / `refresh_token_encrypted` are
AES-256-GCM encrypted by `TokenEncryptionService` before being persisted,
using a key derived from `creatoros.security.token-encryption-key`
(SHA-256-hashed into a 256-bit key, so operators can rotate it with a plain
string environment variable rather than managing raw key material). Neither
raw nor encrypted tokens are ever returned from an API response
(`SocialAccountResponse` deliberately has no token field), and no logging
statement anywhere in the codebase logs a token value.

## CORS

Configured from `creatoros.cors.allowed-origins` (comma-separated), applied
via a `CorsConfigurationSource` bean rather than a blanket `@CrossOrigin`.
Defaults to `http://localhost:5173` (the Vite dev server) for local
development.

## Error responses never leak internals

`GlobalExceptionHandler` maps every exception type to a stable
`ApiError { timestamp, status, code, message, path }` shape. Unexpected
exceptions are logged server-side with full detail but return a generic
`INTERNAL_ERROR` message to the client - no stack trace, no exception class
name, ever reaches a response body.

## Secrets

No credential of any kind is committed to source control. `application.yml`
only ever references environment variables with local-dev-safe defaults
(e.g. `${JWT_SECRET:dev-only-insecure-secret-change-me-please-32-bytes-min}`)
so the app is runnable out of the box locally, while `.env.example` documents
every variable a real deployment must override. `docker-compose.yml` uses
Compose's `${VAR:?error message}` syntax for `JWT_SECRET` and
`TOKEN_ENCRYPTION_KEY` specifically, so `docker compose up` fails fast with a
clear message if you forgot to set them, rather than silently running with
the insecure dev default in what looks like a "real" deployment.
