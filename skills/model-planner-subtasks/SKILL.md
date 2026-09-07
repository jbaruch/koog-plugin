---
name: model-planner-subtasks
description: >
  Model a problem domain as a tree of typed subtasks the Koog 1.2 planner can execute —
  the `PlannerNode` data model, parallel vs sequential composition, accessing
  in-flight subtasks through `AIAgentStorage`, retry-on-parse-failure edges, and
  TL;DR compression between phases. Goes deeper than `use-planner` (which only picks
  the planner factory). Use when the user asks to "decompose into subtasks", "model
  the agent's work as a tree", "compose parallel and sequential steps", "retry failed
  subtasks", or describes a richer planner usage than the basic factory.
---

# Model Planner Subtasks Skill

Process steps in order. Do not skip ahead.

## Step 1 — Confirm This Is the Right Depth

`use-planner` covers picking the planner type and basic factory wiring. This skill is for projects that need to model the *plan itself* — the tree of subtasks the planner builds, how its nodes compose, how to access in-flight subtasks during a run, how to retry failures gracefully.

If the user just needs "an LLM-based planner with default behavior", invoke `Skill(skill: "use-planner")`. If they describe a domain in terms of "parallel and sequential subtasks", "retrying a failed step", "inspecting the plan tree", continue here.

Proceed immediately to Step 2.

## Step 2 — Understand the `PlannerNode` Tree

The planner represents a plan as a **tree of `PlannerNode`s**:

- **Leaf nodes** — concrete actions the planner-driven agent executes (typically an LLM call or a tool invocation)
- **Sequential composite nodes** — children run in order, each consuming the previous's output
- **Parallel composite nodes** — children run concurrently and their outputs are merged

Building blocks (from `agents-planner`):

```kotlin
import ai.koog.agents.planner.PlannerNode

val plan = PlannerNode.sequential(
    PlannerNode.leaf("classify", description = "..."),
    PlannerNode.parallel(
        PlannerNode.leaf("lookup_user", description = "..."),
        PlannerNode.leaf("lookup_account", description = "..."),
    ),
    PlannerNode.leaf("respond", description = "..."),
)
```

For LLM-based planners (`Planners.llmBased`), the tree is *generated* by the LLM from the goal — your code doesn't build it explicitly. For static planners or when you want to pre-seed structure, build it directly and pass to the strategy.

Proceed immediately to Step 3.

## Step 3 — Access In-Flight Subtasks Through Storage

Use `AIAgentStorage` to track in-flight planner subtasks across nodes. The pattern from the in-repo example:

```kotlin
import ai.koog.agents.core.agent.context.createStorageKey

val unfinishedNodesKey = createStorageKey<MutableList<PlannerNode.Builder.Reference>>("unfinishedNodes")
val currentNodeKey = createStorageKey<PlannerNode.Builder.Reference>("currentNode")

// inside a strategy node body, while iterating subtasks:
val unfinished = storage.get(unfinishedNodesKey) ?: mutableListOf()
val next = unfinished.removeFirstOrNull() ?: return null
storage.set(currentNodeKey, next)
storage.set(unfinishedNodesKey, unfinished)
```

`PlannerNode.Builder.Reference` is the planner's identifier for a subtask in the tree. Keep it in storage to maintain "where am I in the plan" across nodes.

In 1.0 storage is serialized into checkpoints — references survive a restart if checkpointing is on (`add-persistence`).

Proceed immediately to Step 4.

## Step 4 — Add Retry-on-Parse-Failure Edges

When a subtask requires the LLM to produce structured output (a JSON plan revision, a typed result), parse failures should retry the subtask, not abort the whole plan:

```kotlin
val executeSubtask by nodeLLMRequest()
val parseResult by node<Message.User, ParsedSubtaskResult?>("parse") { msg ->
    runCatching { parseStructured<ParsedSubtaskResult>(msg.content) }.getOrNull()
}

edge(executeSubtask forwardTo parseResult)
edge(parseResult forwardTo continueWithResult onCondition { it != null })
edge(parseResult forwardTo executeSubtask onCondition { it == null } transformed { "Re-emit the plan revision as valid JSON." })
```

The retry edge feeds a corrective prompt back into the same subtask node. Cap retries — usually 2–3 — by counting attempts in storage; uncapped retries on a misformatted LLM response burn budget fast.

Proceed immediately to Step 5.

## Step 5 — Compress History Between Phases

Long planner runs accumulate enormous history (every subtask's LLM round-trip lands in the conversation). Without compression you hit context limits halfway through. Compress at phase boundaries — between "planning" and "execution", or every N completed subtasks:

```kotlin
val phaseBoundary by node<Unit, Unit>("phase-boundary") {
    llm.writeSession {
        replaceHistoryWithTLDR()
    }
}
```

Pick the compression points by looking at the plan tree — boundaries at the end of a parallel block or between sequential phases. See `manage-state` for the broader compression vocabulary.

Proceed immediately to Step 6.

## Step 6 — Inspect the Plan Tree at Runtime

For debugging and observability, expose the in-flight plan as a side channel:

- Capture the tree (or its current cursor position) in `AIAgentStorage` at every node entry
- Forward it to the event handler — `handle-agent-events`'s `onNodeEntry` (or equivalent) callback can dump the plan state to stdout for live demos
- Persist it via the trace feature (`trace-agent-internals`) for post-mortem analysis

For production, surface key counters (subtasks completed, subtasks failed, retries used) as OpenTelemetry custom metrics on top of the built-in `gen_ai.*` set.

Reference: `examples/simple-examples/.../planner/PlannerAgentExample.kt` — the richest demonstration of all six steps in working code. The example's inline `parse` is a `TODO()` skeleton; the rest of the plumbing (storage keys, retry edges, history compression) is real.

Finish here.
