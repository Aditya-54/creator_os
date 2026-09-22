# AI and MCP

## The core rule: the model never touches the database

```text
User question
     |
     v
ChatClient (Spring AI) + AnalyticsTools as callable tools
     |
     v
LLM decides which tool(s) to call, with which arguments
     |
     v
Tool method -> existing, tested analytics service -> Postgres
     |
     v
Structured result returned to the LLM
     |
     v
LLM synthesizes an answer, required to cite the returned numbers
```

`AnalyticsTools` (`com.creatoros.ai.tools.AnalyticsTools`) is the *only*
surface an LLM can use to affect or observe application state. Every method
is `@Tool`-annotated (Spring AI), delegates straight to an existing analytics
service (`AnalyticsService`, `TopicAnalyticsService`, `RegionAnalyticsService`,
`BrandAnalyticsService`, `ExplainSpikeService`), and returns a plain DTO.
There is no method on this class - or reachable from it - that runs
arbitrary SQL or accepts a free-text query. This is what makes the answers
evidence-grounded rather than merely LLM-plausible: the model can misinterpret
a number, but it cannot invent one that no tool call produced.

## Provider abstraction and graceful degradation

`AiProvider` is the seam:

```java
public interface AiProvider {
    boolean isConfigured();
    String name();
    String answer(String systemPrompt, String userQuestion, List<Object> toolObjects);
}
```

`AiProviderConfig` selects the implementation from `creatoros.ai.provider`
(`none` / `openai` / `anthropic`) at startup:

- If the selected provider's api-key (`spring.ai.openai.api-key` /
  `spring.ai.anthropic.api-key`) is blank, or provider is `none`/unset, the
  bean is `NoOpAiProvider`.
- If a key is present, `AiProviderConfig` tries to build a
  `SpringAiChatProvider` wrapping the corresponding Spring AI `ChatModel`
  bean (`OpenAiChatModel` / `AnthropicChatModel`, looked up lazily via
  `ObjectProvider` so a missing bean doesn't fail application startup) -
  **any** exception during that attempt is caught and logged, and the
  application falls back to `NoOpAiProvider` rather than failing to start.

This is deliberately defensive: a misconfigured or unreachable AI provider
must never take down the rest of the application (section 5 of the product
spec - "the entire application should still run if no LLM API key is
configured"). `AiAnalystService.query` additionally catches any exception
from an actually-configured provider's `answer()` call (network failure,
expired key, rate limit) and falls back to the user's plain analytics
overview instead of returning a 500.

## What "not configured" looks like to a user

`POST /api/ai/query` always returns `200 OK` with a structured
`AiQueryResponse`. When no provider is usable, `aiGenerated` is `false`, the
`answer` field explains why in plain language, and `fallbackOverview` is
populated with the same data `GET /api/analytics/overview` would return - so
the endpoint is still useful, not just an error message, exactly as section 5
requires ("rule-based explanations can be used where appropriate").

## MCP server

`spring-ai-starter-mcp-server-webmvc` auto-configures a Model Context
Protocol server from any `ToolCallbackProvider` bean; `McpToolConfig`
registers one wrapping `AnalyticsTools`:

```java
@Bean
public ToolCallbackProvider analyticsToolCallbackProvider(AnalyticsTools analyticsTools) {
    return MethodToolCallbackProvider.builder().toolObjects(analyticsTools).build();
}
```

Point any MCP-compatible client (Claude Desktop, etc.) at this application's
SSE endpoint (`/sse` by default) to call `get_content_performance`,
`get_growth_timeline`, `get_viral_events`, `get_regional_performance`,
`get_topic_performance`, `compare_platforms`, `compare_content`,
`get_brand_patterns`, `get_historical_patterns`, and `get_content_metadata`
directly - the exact same tools the in-app AI analyst uses.

### Known trust tradeoff

Every tool method takes `userId` (and `contentId` where relevant) as an
**explicit string argument** rather than reading it from a security context.
This is necessary for the standalone MCP server: an external MCP client has
no HTTP session or JWT to carry, so there is no "current user" to infer.
`SecurityConfig` exempts `/sse/**` and `/mcp/**` from JWT authentication for
the same reason.

The in-app AI analyst (`/api/ai/query`, which *is* authenticated) reuses this
same tool surface for architectural consistency, and tells the model the
authenticated caller's own `userId` in the system prompt, instructing it to
always use that exact value. In the worst case (a successful prompt-injection
attack against the model) this trusts the LLM not to substitute a different
user's id into a tool call - a real, deliberately-accepted simplification for
a portfolio project, not something to carry into a production multi-tenant
system without adding server-side authorization per tool call.

**Consequence:** do not expose the MCP server on a public network as-is. It
is intended for local/trusted use - e.g. running the API locally and pointing
Claude Desktop's MCP config at `http://localhost:8080/sse` during a demo.

## Why not let the AI assign topics?

`TopicExtractionService` is deliberately rule-based (whole-word keyword
matching), not LLM-based, so topic assignment - which analytics depend on -
works identically whether or not an AI provider is configured. `TopicSource.AI`
already exists in the schema for a future LLM-assisted classifier to
populate additional topics without changing anything that reads
`content_topics`.
