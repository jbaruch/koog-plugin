---
name: use-planner
description: >
  Pick and wire a planner-driven Koog 1.2 agent — either LLM-based (the LLM picks the
  next action each turn, optionally with a critic loop) or GOAP (a classical planner
  searches a typed state space toward a goal). Pulls `ai.koog:agents-planner`, constructs
  the planner strategy, and wires it into `AIAgent(...)`. Use when the user asks to
  "use a planner", "let the agent plan", "use GOAP", "build a planning agent",
  names any of `Planners.llmBased`, `Planners.llmBasedWithCritic`, `Planners.goap`,
  `PlannerAIAgent`, `agents-planner`, or describes an open-ended task whose step
  sequence depends on runtime context.
---

# Use Planner Skill

This skill is an action router — pick the step that matches the user's intent and execute only that step. Do not run other steps; do not parallelize.

Chaining exception (exhaustive — overrides "Do not run other steps" only as listed):

- Step 1 is the entry point. It either redirects (via `Skill(skill: "author-strategy")`) and finishes, or chains to Step 2 / Step 3 to wire the picked planner variant
- Steps 2 and 3 are terminal — execute one, then finish. Do not chain Step 2 to Step 3 or vice versa
- Direct entry into Step 2 or Step 3 is permitted only when the caller already determined the variant; otherwise enter via Step 1

Available actions:

- **Step 1** — Confirm the planner is the right primitive. If not, redirect via `Skill(skill: "author-strategy")` and finish. If yes, pick LLM-based or GOAP and chain into the relevant Step (2 or 3)
- **Step 2** — LLM-based planner (`Planners.llmBased` or `Planners.llmBasedWithCritic`)
- **Step 3** — GOAP planner (`Planners.goap`)

## Step 1 — Confirm the Planner Fits

The graph DSL (`strategy { ... }`) is the right default. A planner is the right choice when **any** of these are true:

- The action ordering genuinely depends on runtime findings (the next step depends on what the LLM saw in the last one) — not just on which tool the LLM picked
- The action space is large enough that hardcoding edges would be a maintenance burden
- You're willing to trade extra LLM round-trips for autonomy

If the user's description matches "I know the topology, the LLM just picks tools within nodes", redirect:

- Invoke `Skill(skill: "author-strategy")` and run it end-to-end through its Step 8
- Write the graph DSL code to disk per `author-strategy`'s Step 8
- Add a top-of-file comment in the produced strategy file naming the topology as the disqualifying signal for a planner
- Add a second top-of-file comment naming the extra LLM round-trips a planner would have added
- Add a third top-of-file comment acknowledging the developer's "planning" wording and stating that a fixed-topology workflow is graph-DSL territory, not a planner
- Finish here — do not continue into planner-variant selection or Step 2 / Step 3

**Only if you did NOT redirect above** (the planner genuinely fits), pick the variant from the user's description without blocking on a clarifying question:

- LLM-based (default) — pick when ordering depends on runtime findings and state is unstructured prose. Step 2 also shows the optional critic-loop sub-variant for output-quality grading
- GOAP — pick only when the user names "GOAP", "classical planner", "state space search", or supplies a typed `data class` state and precondition/effect pairs

Proceed to Step 2 for LLM-based, Step 3 for GOAP. This handoff applies only when the planner fits; the redirect branch above already finished.

## Step 2 — LLM-Based Planner

Add the planner module to `build.gradle.kts`:

```kotlin
implementation("ai.koog:agents-planner:1.2.0-beta")
```

Construct the planner strategy and wire into `AIAgent(...)`:

```kotlin
import ai.koog.agents.planner.Planners
import ai.koog.agents.core.agent.AIAgent

val strategy = Planners.llmBased("myStrategy")
// or with a critic loop for output quality:
// val strategy = Planners.llmBasedWithCritic("myStrategy")

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    toolRegistry = ...,
    systemPrompt = "...",
    strategy = strategy,
    maxIterations = 400,  // planners chain many LLM steps — default 50 is far too low
)
```

The critic variant adds an inner verify loop where a second LLM call grades each planner step before it's accepted. Use when output quality matters more than throughput; expect roughly 2× the LLM cost.

`Planners.llmBased(name)` is the new constructor — the old `AIAgentPlannerStrategy.builder()` is gone in 1.0 (#1997).

Write to disk with explicit `Path:` labels (same convention as `scaffold-agent`). Do not respond with prose only.

- `Path: build.gradle.kts` — the `ai.koog:agents-planner` dependency
- `Path: src/main/kotlin/com/example/Main.kt` — the agent construction with the planner strategy and the raised `maxIterations`

Create the files if they do not exist.

Finish here.

## Step 3 — GOAP Planner

GOAP needs a **typed, serializable state class** and a set of actions defined as `(precondition, effect)` pairs. If the state cannot be expressed as a `data class` with a finite set of fields, GOAP is the wrong tool — use Step 2.

Add the planner module:

```kotlin
implementation("ai.koog:agents-planner:1.2.0-beta")
```

Define the state and the planner:

```kotlin
import ai.koog.agents.planner.Planners
import kotlinx.serialization.Serializable

@Serializable
data class MyState(
    val hasItem: Boolean = false,
    val location: String = "start",
)

val strategy = Planners.goap("myStrategy", ::MyState) {
    action(
        name = "pickup",
        precondition = { s -> !s.hasItem && s.location == "warehouse" },
        belief = { s -> s.copy(hasItem = true) }
    ) { _, s ->
        // actual side-effect during execution
        s.copy(hasItem = true)
    }

    action(
        name = "move_to_warehouse",
        precondition = { s -> s.location != "warehouse" },
        belief = { s -> s.copy(location = "warehouse") }
    ) { _, s -> s.copy(location = "warehouse") }

    goal("haveItem", condition = { s -> s.hasItem })
}
```

`belief` is the planner's *predicted* effect (used during planning); the action body is the *actual* effect at execution time. They usually match but can diverge — e.g., the actual effect may also update non-state-tracked fields.

Wire into the agent:

```kotlin
val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    toolRegistry = ...,
    systemPrompt = "...",
    strategy = strategy,
    maxIterations = 100,
)
```

Write to disk with explicit `Path:` labels (same convention as `scaffold-agent`). Do not respond with prose only.

- `Path: build.gradle.kts` — the `ai.koog:agents-planner` dependency
- `Path: src/main/kotlin/com/example/Main.kt` — the state class, the GOAP planner, and the agent construction

Create the files if they do not exist.

Finish here.
