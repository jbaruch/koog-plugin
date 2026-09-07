---
name: trace-agent-internals
description: >
  Install the `agents-features-trace` feature to capture detailed internal trace
  events from a Koog 1.2 agent — node entries, edge transitions, planner decisions,
  feature lifecycle. Distinct from OpenTelemetry (production signal, GenAI vocabulary)
  and event handlers (high-level callbacks). Use when the user asks to "debug what
  the strategy is doing", "trace internal agent decisions", "see why the planner
  picked that step", or describes deep diagnostic needs.
---

# Trace Agent Internals Skill

Process steps in order. Do not skip ahead.

## Step 1 — Confirm This Is the Right Layer

Three overlapping observability layers — pick by purpose:

- **OpenTelemetry** (`add-observability`) — production signal. GenAI-standard metrics, dashboards, low cardinality. Keep it on
- **Event handlers** (`handle-agent-events`) — high-level lifecycle callbacks (tool start/end, agent finish, LLM request). Lightweight; good for demos and stdout traces
- **Trace feature** (this skill) — deep diagnostics. Node entries/exits, edge predicate evaluations, planner internal state, feature install/uninstall. Heavy enough that you don't want it on in prod

If the user wants "production dashboards" → `add-observability`. "Live stdout demo trace" → `handle-agent-events`. "I can't figure out why the agent is looping at this edge" → this skill.

Proceed immediately to Step 2.

## Step 2 — Add the Dependency

```kotlin
implementation("ai.koog:agents-features-trace:1.2.0")
```

Consider scoping it to a debug build variant (Gradle `debugImplementation` for Android, a `debug` source set for plain JVM) — production binaries don't need this feature on the classpath.

Proceed immediately to Step 3.

## Step 3 — Install the Feature

```kotlin
import ai.koog.agents.features.trace.Trace

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = "...",
) {
    install(Trace) {
        // sink — where trace events go
        sink = TraceSink.stdout()
        // or:
        // sink = TraceSink.file(Paths.get("trace.jsonl"))
        // or write a custom sink that forwards to a logger

        // event filters — drop noisy categories you don't need
        includeCategories = setOf(
            TraceCategory.NODE_ENTRY,
            TraceCategory.NODE_EXIT,
            TraceCategory.EDGE_EVALUATION,
            TraceCategory.PLANNER_DECISION,
        )
    }
}
```

The trace feature emits to a sink synchronously — keep the sink fast (stdout, file, in-memory). For a slow sink (network, JSON-formatting per event), wrap it in a buffered/async layer or you'll throttle the agent.

Proceed immediately to Step 4.

## Step 4 — Read the Trace

A typical trace event line includes: timestamp, category, node name (if applicable), event payload (predicate result, planner choice, LLM token count). Use this to answer questions like:

- "Why did the agent take edge X instead of edge Y?" → `EDGE_EVALUATION` events show the predicate result
- "Why did the planner pick this action?" → `PLANNER_DECISION` events show the candidates considered and the chosen one with score
- "Where does the agent loop?" → repeating `NODE_ENTRY` patterns for the same node sequence

Combine with the test mocked-executor pattern (`test-koog-agents`) to capture a deterministic trace of a known bug — that's the fastest path to a fix.

Proceed immediately to Step 5.

## Step 5 — Don't Confuse with OpenTelemetry's Trace API

OpenTelemetry also has the word "trace" — it refers to **distributed traces** (request flow across services). This Koog feature emits **internal traces** (agent flow inside one process). Both can coexist; they don't compete. If the user mentions "trace" without context, ask whether they want internal diagnostics (this skill) or distributed traces across services (`add-observability` + OTLP).

Finish here.
