---
name: snapshot-and-restore
description: >
  Save an explicit Koog 1.3 checkpoint and replay or fork from it. Use when the user
  asks to snapshot at a chosen decision point, compare branches from saved state,
  or build replay tooling. Uses Persistence with automatic checkpointing disabled.
---

# Snapshot and Restore Skill

Process steps in order. Do not skip ahead.

## Step 1 — Choose Manual or Automatic Checkpoints

Koog 1.3 uses `Persistence` from `ai.koog.agents.snapshot.feature` for both modes.
There is no separate `Snapshot` feature, `snapshot()` or `runFromSnapshot()` API.

For automatic crash recovery without explicit save calls, invoke
`Skill(skill: "add-persistence")`, deliver its durable backend and stable-session
run path, and explain that automatic checkpointing is the requested mode. Finish here.
For chosen save points and forks, proceed immediately to Step 2.

## Step 2 — Implement the Fork

Add `ai.koog:agents-features-snapshot:1.3.0` to `build.gradle.kts`. Read `skills/snapshot-and-restore/references/checkpoint-fork.md` and write its
factory and comparison helper to `src/main/kotlin/com/example/CheckpointFork.kt`, adapting the named prefix and
continuation nodes to the developer's existing graph.


Run the initial prefix once and retain the `AgentCheckpointData` supplied to
`onCheckpoint`. Pass it to `compareCheckpointBranches`. Replay resumes after the
saved node. The two copies replace its serialized output for the continuation;
changing the ordinary `input` argument alone does not change the restored output.
Use distinct session IDs for independent branches and stable matching graph/node
names. Forks preserve the saved prefix history and typed storage. The capture run finishes
without executing the continuation; only the two restored branches execute it.

In-memory storage is suitable for same-process comparison. For later process
restarts, replace it with a durable provider and retrieve the saved checkpoint by
session and checkpoint ID. Increment checkpoint versions for repeated saves in a
session. Node outputs and typed storage must serialize successfully; custom data
classes require a supported serializer. Invoke `Skill(skill: "manage-state")` for
typed storage. Replay does not undo external side effects; isolate branch effects
or make them idempotent.

Finish here.
