---
name: migrate-from-0-x
description: >
  Migrate a Koog 0.x codebase to 1.0. Koog 1.0 (2026-05-21) removed every
  @Deprecated API in one sweep — 0.x code does not compile against 1.0. Walks through
  construction surface, planner module split, graph DSL renames, Java interop overhaul,
  HTTP transport decoupling, memory APIs, OpenTelemetry, persistence, prompt package
  move, retired models, and JDK/tooling minima. Use when the user asks to "migrate
  from 0.x", "bump Koog to 1.0", "upgrade Koog", or shows code that uses pre-1.0
  APIs (e.g., AIAgent.invoke, AgentMemory, AIAgentPlannerStrategy.builder).
---

# Migrate From 0.x Skill

Process steps in order. Do not skip ahead — but in any single migration only the steps that match the user's code will produce changes; the rest are no-ops the user can skim. Don't chase compile errors blind; start with Step 1 and work down.

## Step 1 — Bump the Build Toolchain

JDK 17 is the minimum in 1.0 (#1931). Targeting 8/11 fails to compile.

```kotlin
kotlin {
    jvmToolchain(17)
}
```

Kotlin 2.3.10 is the recommended version. Android consumers must set `android.useAndroidX=true` in `gradle.properties`. Bump these before bumping the Koog dependency — otherwise the build fails on the toolchain check before you can read the API errors.

Proceed immediately to Step 2.

## Step 2 — Bump Coordinates

Change every `ai.koog:*` artifact to the current release. Don't leave mixed versions — pre-1.0 and 1.x do not interoperate. Koog publishes on two version lines and the umbrella's version does not exist for every module, so take each coordinate from `rules/module-coordinates.md` rather than applying one version string across the board.

If the codebase pulled Ktor types from Koog packages, that route was closed in 1.0 — add `ai.koog:http-client-ktor:1.2.0` explicitly. Run `./gradlew dependencies | grep -i ktor` if unsure.

Proceed immediately to Step 3.

## Step 3 — Fix Construction Sites

- `AIAgent.invoke(...)` → top-level `AIAgent(...)` factory (#1882). Call site reads the same; the resolution target changed
- Direct constructors of `GraphAIAgent` / `FunctionalAIAgent` / `PlannerAIAgent` → top-level `AIAgent(...)` factory
- `AIAgentService.invoke(...)`, `ToolRegistry.invoke(...)`, `RollbackToolRegistry.invoke(...)`, `AIAgentPlannerStrategy.invoke(...)` → top-level functions

Proceed immediately to Step 4.

## Step 4 — Planner Module Split

If the codebase uses planners, add the new module:

```kotlin
implementation("ai.koog:agents-planner:1.2.0-beta")
```

Then:

- `AIAgentPlannerStrategy.builder()` and `AIAgentPlannerStrategy.goap()` → `AIAgentPlannerStrategy(name, planner)`, `Planners.goap(...)`, `Planners.llmBased(...)`, `Planners.llmBasedWithCritic(...)`
- Subclasses of `AIAgentPlanner` / `JavaAIAgentPlanner` need new `initializeState` and `provideOutput` overrides

Proceed immediately to Step 5.

## Step 5 — Graph DSL Renames (#2035)

If the codebase uses the `strategy { ... }` DSL:

- String-input nodes: `nodeLLMRequest*`, `nodeLLMModerateText`
- `Message.User`-input nodes: `nodeLLMSendMessage*`, `nodeLLMSendToolResults*`, `nodeLLMModerateMessage`
- `nodeExecuteTools()` now returns `ReceivedToolResults` directly — the auto-writeback variant is gone. Chain explicitly to `nodeLLMSendToolResults` (`Skill(skill: "author-strategy")` covers the full chain)

Compile-time errors will surface these; the fix is the rename, not a redesign.

Proceed immediately to Step 6.

## Step 6 — Java Interop (#2005)

If the codebase has Java consumers of Koog APIs:

- `xxx` from Java, `xxxBlocking` from Kotlin — `ExecutorService` / `Executor` parameters were removed throughout
- `NonSuspendAIAgentStrategy` → `AIAgentStrategyBlocking`
- `NonSuspendAIAgentFunctionalStrategy` → `AIAgentFunctionalStrategyBlocking`
- `FeatureMessageProcessor.javaNonSuspend*` → `FeatureMessageProcessor.*Blocking`
- Kotlin↔Java↔Kotlin re-entrant calls no longer deadlock (#1945) — remove the hand-rolled workaround if it exists

Proceed immediately to Step 7.

## Step 7 — HTTP Transport (#2006)

If the code constructs `LLMClient` instances directly:

- `LLMClient` constructors now take `KoogHttpClient.Factory`, not a Ktor `HttpClient`
- `SimplePromptExecutorsKt` was renamed `SimplePromptExecutors` — update Java imports
- `OllamaClient.baseUrl` parameter removed — pass it via `KoogHttpClient` configuration

For default JVM use, `simpleOpenAIExecutor(apiKey)` (and its siblings) still resolve without a factory argument because the JVM auto-discovers one — code that uses only the simple executors usually doesn't need changes.

Proceed immediately to Step 8.

## Step 8 — Memory APIs

If the code installs the `AgentMemory` feature:

- `AgentMemory` was **removed**. Install `LongTermMemory` instead — invoke `Skill(skill: "manage-state")` for the install steps
- Renames inside `LongTermMemory`: `QueryExtractor` → `SearchQueryProvider`; `ExtractionStrategy` → `DocumentExtractor`; `IngestionTiming` is gone
- `RetrieveFactsFromHistory` was extracted out of `AgentMemory` — now standalone in `agents-core`

Proceed immediately to Step 9.

## Step 9 — OpenTelemetry

If the code installs `OpenTelemetry`:

- JVM-only knobs (`addSpanExporter`, `addMetricExporter`, `addMetricFilter`) moved to `OpenTelemetryConfigJvm` — update imports
- `addResourceAttributes` signature changed to `Map<String, Any>`
- `SpanEndStatus` → `StatusData`
- The `ai.koog.agents.features.opentelemetry.event` package and event APIs on `GenAIAgentSpan` (`events`, `addEvent`, `addEvents`, `removeEvent`) were **removed**. Replace with span attributes (`gen_ai.input.messages` / `gen_ai.output.messages`)
- JVM shutdown hook no longer installed automatically — opt in via `setShutdownOnAgentClose(true)`

Proceed immediately to Step 10.

## Step 10 — Persistence

If the code uses persistence/checkpoints:

- Type spelling: `Persistency*` → `Persistence*`
- `AIAgentPipeline` / `AIAgentPipelineImpl` → `AIAgentPipelineAPI` + `AIAgentGraphPipeline` / `AIAgentPlannerPipeline`
- `AgentCheckpointData` shape changed — `nodePath`, `lastInput`, `lastOutput` moved inside `properties: JSONObject`
- `AIAgentStorageKey` equality is now name-based
- `AIAgentStorage()` no-arg constructor replaced by `AIAgentStorage(serializer)`
- `AIAgentStorage.toMap()` removed

Proceed immediately to Step 11.

## Step 11 — Prompt Package Move

- `ai.koog.prompt.dsl.Prompt` → `ai.koog.prompt.Prompt` (#2022)
- The `prompt { ... }` builder DSL stays in `ai.koog.prompt.dsl`. Imports for the builder don't change; imports for `Prompt` itself do

Proceed immediately to Step 12.

## Step 12 — Clocks

If the code passes a `Clock` to Koog APIs:

- `kotlin.time.Clock` parameters replaced by `KoogClock` (#1925). Update test doubles too — `KoogClock.System` is the default

Proceed immediately to Step 13.

## Step 13 — Retired Models

Check model references in the code:

- `AnthropicModels.Haiku_3` — removed; pick a current Anthropic entry
- `BedrockModels.AnthropicClaude3Haiku` — removed
- Several older Google/DeepSeek model entries were retired

New in 1.0 worth knowing: `AnthropicModels.Opus_4_7`, `GPT-5.5`/`GPT-5.5 Pro`, DeepSeek V4 Flash/Pro, Bedrock Kimi K 2.5 / MiniMax 2.5 / Gemma 3 / GPT OSS, Ollama `gpt-oss` / `qwen3.5`, LiteRT (local Google).

Proceed immediately to Step 14.

## Step 14 — Build

Run `./gradlew build`. The earlier steps cover the structural changes; remaining errors are usually unrelated to migration (project-specific code paths). If a compile error still references a Koog symbol, re-check Steps 3–13 — the symbol was probably renamed in one of them.

Finish here.
