---
name: enable-prompt-caching
description: >
  Enable Anthropic prompt caching for a Koog 1.2 agent — automatic caching is on by
  default in 1.0, but explicit `cacheControl` breakpoints let you control which parts
  of long prompts get cached. Surfaces cache-hit metrics through the OpenTelemetry
  token-usage span. Use when the user asks to "enable prompt caching", "reduce
  Anthropic costs", "add cache_control", "set cache breakpoints", or describes
  expensive repeated calls with shared system prompt content.
---

# Enable Prompt Caching Skill

Process steps in order. Do not skip ahead.

## Step 1 — Confirm the Provider

Prompt caching, as covered here, is the **Anthropic** caching feature (cache breakpoints in the messages API; cache hits billed at reduced rates). Koog 1.2 enables automatic caching when calling Anthropic models.

If the user is on OpenAI, Google, or another provider, redirect:

- Provider-side prompt caching for OpenAI/Google is provider-driven; check the provider's billing docs
- For in-process caching of repeated calls (independent of provider), invoke `Skill(skill: "cache-llm-calls")` — uses `prompt-executor-cached`

If the user is on Anthropic, proceed to Step 2.

## Step 2 — Automatic Caching (Already On)

Koog 1.2 turned on automatic Anthropic prompt caching by default. Long system prompts and repeated tool definitions are cached without code changes.

To verify it's working, check the token-usage span attributes after a few runs:

- Look for non-zero `cache_creation_input_tokens` and `cache_read_input_tokens` in the `gen_ai.client.token.usage` metric
- The first call populates the cache (`cache_creation_input_tokens > 0`); subsequent calls within Anthropic's TTL hit it (`cache_read_input_tokens > 0`)

If you don't have telemetry installed, invoke `Skill(skill: "add-observability")` first. Without it you can't tell whether caching is helping.

If automatic caching is sufficient, finish here. Continue to Step 3 only if the user needs explicit breakpoints.

## Step 3 — Explicit `cacheControl` Breakpoints

Use when the prompt has a clear "stable prefix + volatile suffix" shape and automatic caching is missing the optimal split — e.g., a long static system prompt followed by short variable user inputs.

The 1.0 prompt DSL exposes `cacheControl` on prompt segments. Set a breakpoint at the boundary between stable and volatile content:

```kotlin
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.cache.CacheControl

val cachedPrompt = prompt("triage") {
    system("""
        You are a GitHub triage assistant.
        [... long stable instructions ...]
    """.trimIndent(), cacheControl = CacheControl.Ephemeral)

    // Anything after this point is treated as volatile and not cached
}
```

`CacheControl.Ephemeral` is the standard 5-minute Anthropic cache tier. Anthropic's caching has minimum-size requirements (typically ≥1024 tokens for Sonnet/Opus, ≥2048 for Haiku); breakpoints on shorter content are silently ignored by the API — no error, just no savings.

The deprecated `user(..., cacheable = true)` form from pre-1.0 was removed; the `cacheControl` parameter is the new explicit surface.

Finish here.
