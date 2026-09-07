---
name: use-functional-agent
description: >
  Use `FunctionalAIAgent` — the third concrete agent subtype in Koog 1.2 (alongside
  `GraphAIAgent` and `PlannerAIAgent`). Wraps a single suspending block, no graph DSL,
  no planner — just programmer-written logic that calls the LLM and tools directly.
  Use when the user asks to "skip the graph DSL", "write the agent body as plain code",
  "use AIAgentFunctionalStrategy", or describes a one-shot agent shape that doesn't
  warrant a topology.
---

# Use Functional Agent Skill

Process steps in order. Do not skip ahead.

## Step 1 — Confirm This Is the Right Shape

Three agent subtypes in 1.0:

- **`GraphAIAgent`** — wires a strategy graph (default via `singleRunStrategy()`, custom via `strategy { ... }`). What `AIAgent(...)` returns by default
- **`PlannerAIAgent`** — LLM-based or GOAP planner picks steps at runtime (`use-planner`)
- **`FunctionalAIAgent`** — wraps a single suspending block. No graph, no planner — programmer writes the body directly

Use functional when:

- The agent's logic is one-shot and short — "call LLM with this prompt, return the result", with maybe one transformation
- You want full Kotlin control flow without learning the strategy DSL
- The strategy DSL feels like overkill for what amounts to a few lines

Don't use functional when:

- The flow needs visible topology (subgraphs, conditional edges, loops) — that's `GraphAIAgent`
- The step order depends on runtime context — that's `PlannerAIAgent`
- You need feature composition that depends on graph entry points — most features assume `GraphAIAgent.FeatureContext`

Proceed immediately to Step 2.

## Step 2 — Use the Functional Factory

The top-level `AIAgent(...)` factory has overloads for functional strategies. Pass an `AIAgentFunctionalStrategy` (or the inline block factory):

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgentFunctionalStrategy

val agent: FunctionalAIAgent<String, String> = AIAgent.functional(
    promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    llmModel = OpenAIModels.Chat.GPT4o,
    systemPrompt = "You translate inputs to formal English.",
    strategy = AIAgentFunctionalStrategy { input ->
        // input is the agent's String input; return the String output
        val response = llm.requestSingle(input)
        response.content.trim()
    },
)

val out: String = agent.run("yo this kinda sucks")
```

Inside the strategy block, `this` is `AIAgentContext` — `llm`, `storage`, `clock` are available. You write the LLM round-trip directly; no `nodeLLMRequest`, no edges.

Proceed immediately to Step 3.

## Step 3 — Mind the Feature Limits

Some features expect `GraphAIAgent.FeatureContext` and don't compose with `FunctionalAIAgent`. Specifically:

- Strategy-graph-aware features (subgraph hooks, edge interceptors) — N/A; there's no graph
- Persistence checkpoints — still work for storage, but "resume from a specific node" doesn't apply (no nodes)
- The `Trace` feature's node/edge categories produce no events for a functional agent — only top-level lifecycle events fire

`OpenTelemetry`, `LongTermMemory`, `Tokenizer`, `EventHandler` work as usual.

If you find yourself reaching for graph-aware features inside a functional agent, that's a signal you wanted `GraphAIAgent` all along — swap to `strategy { ... }`.

Proceed immediately to Step 4.

## Step 4 — Hand Off

Functional agents are the cheapest entry point — they avoid the DSL learning curve and read like normal Kotlin. Use them for:

- Glue agents (one LLM call wrapped with input/output processing)
- Demo skeletons before promoting to a real graph
- Tests of features that don't need graph behavior

When the body starts to grow conditional branches, retries, or tool loops, promote to `GraphAIAgent` with a `strategy { ... }`. Don't try to push functional further than it goes — the topology you'd hand-roll inside a single suspending block becomes the graph DSL by another name, and you lose the inspectability the DSL gives.

Finish here.
