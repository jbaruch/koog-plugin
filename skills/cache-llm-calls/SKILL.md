---
name: cache-llm-calls
description: >
  Add in-process caching of LLM calls to a Koog 1.2 agent via `prompt-executor-cached` —
  cache whole prompt→response pairs locally so identical calls skip the API. Distinct
  from provider-side Anthropic prompt caching (covered by `enable-prompt-caching`).
  Backends include in-memory (default), file-based, and Redis. Use when the user asks
  to "cache LLM responses", "avoid duplicate API calls", "add a response cache",
  "cache to Redis", or describes repeated identical prompts in dev/test.
---

# Cache LLM Calls Skill

Process steps in order. Do not skip ahead.

## Step 1 — Confirm This Is the Right Cache Layer

Two distinct caching concepts in Koog:

- **`prompt-executor-cached`** — **in-process** cache of full prompt→response pairs. Skips the API entirely on cache hits. Useful for: deterministic dev/test runs, replaying conversations, avoiding cost on identical retries
- **Anthropic prompt caching** — **provider-side** cache of prompt prefixes; still makes the API call but Anthropic bills cache hits at reduced rates. Useful for: long stable system prompts at high call volume

If the user wants to avoid the API call entirely → this skill. If they want lower-cost API calls with full LLM behavior → invoke `Skill(skill: "enable-prompt-caching")`.

Proceed immediately to Step 2.

## Step 2 — Pick a Cache Backend

Ask the user which backend fits their need:

- **In-memory** — process-local. Fastest, loses cache on restart. Default for tests and short runs
- **File** — disk-backed (`prompt-cache-files`). Survives restart. Good for dev caching across multiple runs of the same script
- **Redis** — shared across processes (`prompt-cache-redis`). Good for multi-instance deployments where workers should share cache
- **Model-aware** (`prompt-cache-model`) — partitions cache by model so swapping models doesn't poison results

If the user is in a single-process dev loop, default to file. For tests, default to in-memory. For multi-instance prod, Redis.

Proceed immediately to Step 3.

## Step 3 — Add the Dependencies

```kotlin
implementation("ai.koog:prompt-executor-cached:1.2.0")
// pick one backend:
implementation("ai.koog:prompt-cache-files:1.2.0")
// or:
// implementation("ai.koog:prompt-cache-redis:1.2.0-beta")
// or in-memory (no extra dep — bundled with prompt-executor-cached)
```

Proceed immediately to Step 4.

## Step 4 — Wrap the Executor

The cache is a decorator on the prompt executor — wrap the underlying provider executor before passing to `AIAgent(...)`:

```kotlin
import ai.koog.prompt.executor.cached.CachedPromptExecutor
import ai.koog.prompt.executor.cached.FilePromptCache
import java.nio.file.Paths

val rawExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY"))
val cachedExecutor = CachedPromptExecutor(
    delegate = rawExecutor,
    cache = FilePromptCache(directory = Paths.get(".koog-cache")),
)

val agent = AIAgent(
    promptExecutor = cachedExecutor,
    llmModel = OpenAIModels.Chat.GPT4o,
    systemPrompt = "...",
)
```

Cache keying is content-based — same prompt + same model + same parameters hits the cache. Changing any of those produces a miss.

Proceed immediately to Step 5.

## Step 5 — Know the Pitfalls

- **Tool calls are part of the prompt history** — once the agent picks a tool, subsequent calls in the same conversation depend on the tool result. Caching the conversation prefix is safe; caching across different tool-result branches is not (and the content-based key prevents it)
- **Don't cache in production unless the use case demands it** — caching response variability away is fine for replay and tests, harmful for diverse user-facing output
- **Cache invalidation is your problem** — system prompt changes should bust the cache; the cache key includes the system prompt content so this happens automatically, but custom keying strategies must preserve this property
- **`gen_ai.client.token.usage` reports zero on cache hits** — observability shows the cache is working but also shows lower "real" usage; account for this when reading dashboards

Finish here.
