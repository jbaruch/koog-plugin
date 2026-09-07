---
name: add-persistence
description: >
  Add checkpoint-and-resume to a Koog 1.2 agent. Two modes — `runFromCheckpoint`
  for replay-only use without installing a feature, and the full Persistence
  feature when you need rolling checkpoints, replay-with-modifications, or planner-agent
  durability across restarts. Use when the user asks to "make the agent resumable",
  "save progress", "checkpoint the agent", "restart from where it left off", or
  describes a long-running workflow that may be interrupted.
---

# Add Persistence Skill

This skill is an action router — pick the step that matches the user's intent and execute only that step. Do not run other steps; do not parallelize.

Available actions:

- **Step 1** — `runFromCheckpoint` only (replay/restore from a saved checkpoint, no feature install)
- **Step 2** — Persistence feature install (writing checkpoints continuously during a run)

## Step 1 — `runFromCheckpoint` Only

Use when you have a checkpoint payload (typically from a previous run that did install the feature) and you want to resume execution from it — without installing the write-side feature yourself.

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.persistence.AgentCheckpointData

val checkpoint: AgentCheckpointData = loadFromYourStorage(...)

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = "...",
)

val result = agent.runFromCheckpoint(checkpoint)
```

`AgentCheckpointData` shape in 1.0:

- `id`, `sessionId`, `schemaVersion` at the top level
- `properties: JSONObject` contains `nodePath`, `lastInput`, `lastOutput` (moved inside `properties` in 1.0)
- `storage` is the serialized `AIAgentStorage`

If you constructed checkpoints manually under 0.x, the shape is different — extract the moved fields and rebuild before passing.

Finish here.

## Step 2 — Persistence Feature Install

Use when the agent needs to **write** checkpoints continuously during a run — for crash resilience, replay-with-modifications, or to back planner agents that survive restarts.

Add the dependency:

```kotlin
implementation("ai.koog:agents-features-persistence-jdbc:1.2.0")
// or another persistence backend module — JDBC is one of several
```

Install in the agent's trailing lambda:

```kotlin
import ai.koog.agents.features.persistence.Persistence

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = "...",
) {
    install(Persistence) {
        // backend-specific config — JDBC URL + credentials, or in-memory for tests
        // type names use `Persistence*` spelling — `Persistency*` from pre-1.0 was renamed
    }
}
```

Tune checkpoint frequency to the run. Checkpoint every N steps or at phase boundaries. Reserve every-step writes for runs where redoing a single step justifies the overhead.

For planner agents specifically: 1.0 added checkpoint support for planner state (KG-673). `AIAgentStorage` is serialized into checkpoints automatically, so any `createStorageKey<T>` value with a `@Serializable` type rides along — non-serializable types break checkpointing silently (invoke `Skill(skill: "manage-state")` for the serialization constraints).

The corresponding pipeline interfaces also split in 1.0: `AIAgentPipeline` → `AIAgentPipelineAPI` + `AIAgentGraphPipeline` / `AIAgentPlannerPipeline`. Code that referenced the old interface needs updating.

Finish here.
