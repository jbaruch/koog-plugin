---
name: snapshot-and-restore
description: >
  Snapshot a running Koog 1.2 agent's state at arbitrary points and restore later —
  distinct from the persistence checkpoint loop in `add-persistence`. Snapshots are
  caller-triggered; persistence is automatic and continuous. Use when the user asks
  to "snapshot the agent", "save state at this point", "restore from a snapshot",
  or names the `agents-features-snapshot` module.
---

# Snapshot and Restore Skill

Process steps in order. Do not skip ahead.

## Step 1 — Confirm Snapshot Is the Right Tool

Persistence (`add-persistence`) writes checkpoints automatically on a configured schedule (every node, every N steps, every successful turn). Snapshot is **caller-triggered** — you call `snapshot()` when *you* decide it's a useful save point.

Use snapshot when:

- The interesting save points are semantic ("after the user approves the plan"), not periodic
- You want to fork — take a snapshot, try variant A, restore, try variant B
- You're building a debugger or replay tool that needs explicit save/load semantics

If the user's need is "agent should resume after a crash", snapshot is the wrong feature. It is caller-triggered, not automatic. Invoke `Skill(skill: "add-persistence")` and deliver its `install(Persistence)` solution. Add one reply sentence telling the developer snapshot is caller-triggered and the Persistence feature checkpoints automatically. The redirect alone is not the deliverable. Finish here. Do not continue to Step 2. If the need is "save state at this specific point", continue.

Proceed immediately to Step 2.

## Step 2 — Add the Dependency

```kotlin
implementation("ai.koog:agents-features-snapshot:1.2.0")
```

Proceed immediately to Step 3.

## Step 3 — Install the Feature

```kotlin
import ai.koog.agents.features.snapshot.Snapshot

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = "...",
) {
    install(Snapshot) {
        // optional: storage backend (in-memory, disk, JDBC)
    }
}
```

Proceed immediately to Step 4.

## Step 4 — Call the Snapshot API from Code

Inside a node body (or from outside the run via the agent's API), call `snapshot()`:

```kotlin
// inside a node body
val snapshotId = snapshot()        // returns an identifier you store
storeSnapshotId(snapshotId)
```

Restore by passing the snapshot ID to `runFromSnapshot`:

```kotlin
val snapshotId = loadSnapshotId()
val result = agent.runFromSnapshot(snapshotId, additionalInput = null)
```

Snapshots are the typed-storage half of persistence — `AIAgentStorage` rides along automatically (values must be `@Serializable`, see `manage-state`). Non-serializable types break snapshots silently, same as checkpoints.

Forking — take a snapshot, run one branch, restore, run another:

```kotlin
val branchPoint = snapshot()
val resultA = agent.runFromSnapshot(branchPoint, additionalInput = "variant A input")
val resultB = agent.runFromSnapshot(branchPoint, additionalInput = "variant B input")
```

Useful for A/B testing strategy variants without re-running the prefix.

Finish here.
