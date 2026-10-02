---
name: add-persistence
description: >
  Route Koog 1.3 graph checkpoint replay, automatic durable recovery, conversation
  history and explicit forks. Use when the user asks to make an agent resumable,
  restart an interrupted workflow, resume a conversation, or fork from saved state.
---

# Add Persistence Skill

This skill is an action router — pick the step that matches the user's intent and execute only that step. Do not run other steps; do not parallelize.

- **Step 1** — Replay a saved graph checkpoint.
- **Step 2** — Automatic crash recovery for graph or planner agents.
- **Step 3** — Conversation history across visits.
- **Step 4** — Explicit save points and forks.

## Step 1 — Replay a Saved Checkpoint

Add `ai.koog:agents-features-snapshot:1.3.0`. Write the replay helper to
`src/main/kotlin/com/example/CheckpointReplay.kt`.

<!-- compile-example: CheckpointReplay -->
```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.snapshot.feature.AgentCheckpointData
import ai.koog.agents.snapshot.feature.Persistence

suspend fun replayCheckpoint(
    agent: AIAgent<String, String>,
    checkpoint: AgentCheckpointData,
): String {
    requireNotNull(checkpoint.graphProperties) {
        "Supply a graph checkpoint; recover planner checkpoints with installed Persistence and a stable session ID"
    }
    return Persistence.runFromCheckpoint(
        agent = agent,
        input = "",
        checkpoint = checkpoint,
    )
}
```

This helper accepts graph checkpoints only. Koog 1.3 casts restored state to
`GraphAgentContextData`; a planner checkpoint cannot use this helper. Choose Step 2
for planner recovery. The agent must use the matching graph and node names. Replay resumes **after** the
saved node using `graphProperties.lastOutput`; changing `input` does not replace
that saved output. This helper does not require installing Persistence and does
not create new checkpoints. `agent.runFromCheckpoint` is not a Koog 1.3 member.

`AgentCheckpointData` carries `checkpointId`, `createdAt`, `version`, message history,
serialized storage, and graph or planner properties. Graph state uses
`graphProperties.nodePath` and `graphProperties.lastOutput`. The provider indexes
checkpoints by session ID; it is not a field on the checkpoint payload. Load
persisted payloads through the provider rather than constructing an obsolete shape.

Finish here.

## Step 2 — Automatic Durable Checkpoints

Add `ai.koog:agents-features-persistence-jdbc:1.3.0` for JDBC. This includes the
`agents-features-snapshot` module containing Persistence. Add a PostgreSQL JDBC
driver. Preserve the existing graph or planner strategy; the install block applies
to both. Planner agents retain `ai.koog:agents-planner:1.3.0-beta`. The factories in
`skills/add-persistence/references/durable-agents.md` demonstrate the graph overload.
Read that reference and write the selected backend and agent factory to
`src/main/kotlin/com/example/DurableAgent.kt`.

Write the consumer project's `.env.example` with placeholders and documentation
for these settings, all required when using the PostgreSQL factory:

- `CHECKPOINT_JDBC_URL`: JDBC connection URL from the database service's connection settings.
- `CHECKPOINT_DB_USER`: database role provisioned by the database administrator or service.
- `CHECKPOINT_DB_PASSWORD`: that role's credential from the database administrator or credential store.

Run with a stable work-item session ID: `agent.run(input, sessionId = workItemId)`.
After an interruption, recreate the agent with the same graph, durable provider and
session ID and call `run` again. Persistence restores the latest non-tombstone
checkpoint automatically. Completed runs write a tombstone; the same session then
starts a new run. Explicit graph replay uses Step 1. Planner recovery uses installed Persistence
and the same provider/session ID, not the graph-only replay helper. For a supplied
planner checkpoint, save it to that provider under the session ID before running.

For local disk storage, add `ai.koog:agents-features-snapshot:1.3.0` and use this
file factory from `skills/add-persistence/references/durable-agents.md` with a
persistent directory.

The default storage provider is a no-op. In-memory storage does not survive a
process restart. Automatic graph persistence writes after each nontechnical node;
`enableAutomaticPersistence` is a boolean, not an every-N-steps configuration.
Use explicit save points with automatic persistence disabled for coarser frequency.
Custom planners must pass matching non-null `stateType` and `planType` tokens to
the `AIAgentPlanner` base constructor. Built-in `SimpleLLMPlanner` supplies these
tokens. Planner state and plans must serialize with the configured serializer.
Keep node outputs and storage values serializable with the configured serializer;
unserializable outputs can skip a checkpoint. Custom data classes need a supported
serializer, such as `@Serializable` with Kotlinx. Invoke `Skill(skill: "manage-state")`
for typed storage. Checkpoint replay can repeat external side effects; use idempotent
operations or configure rollback tools for the application.

Finish here.

## Step 3 — Conversation History

Invoke `Skill(skill: "persist-chat-history")` and execute its JDBC/provider flow.
Conversation history restores messages across visits; execution checkpoints restore
an interrupted run's position and state.

Finish here.

## Step 4 — Explicit Forks

Invoke `Skill(skill: "snapshot-and-restore")` and execute its manual-save/fork flow.

Finish here.
