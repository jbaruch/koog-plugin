---
name: manage-state
description: >
  Work with Koog 1.2 agent state — typed key-value `storage` on `AIAgentContext`,
  history compression strategies (TL;DR, sliding window, fact retrieval), and the
  `LongTermMemory` feature (which replaces the removed `AgentMemory`) for cross-session
  recall. Use when the user asks to "store state across nodes", "compress conversation
  history", "remember things across sessions", "add long-term memory", or names any
  of these surfaces.
---

# Manage State Skill

This skill is an action router — pick the step that matches the user's intent and execute only that step. Do not run other steps; do not parallelize.

Available actions:

- **Step 1** — Per-run typed storage (`AIAgentStorage` + `createStorageKey`)
- **Step 2** — History compression mid-run (`HistoryCompressionStrategy`)
- **Step 3** — Cross-session memory (`LongTermMemory` feature)

## Step 1 — Per-Run Storage

`storage` on `AIAgentContext` is the typed key-value store. Keys are created once at file scope; values are read/written inside node bodies:

```kotlin
import ai.koog.agents.core.agent.context.createStorageKey

val unfinishedNodesKey = createStorageKey<MutableList<NodeRef>>("unfinishedNodes")
val currentNodeKey = createStorageKey<NodeRef>("currentNode")

// inside a node body — `this` is AIAgentContext
storage.set(currentNodeKey, ref)
val current = storage.get(currentNodeKey)
```

**Constraints (1.0):**

- Values must be `@Serializable` — storage is checkpointed (KG-673). Non-serializable types (thread-locals, open file descriptors, raw clients) break checkpointing silently
- `AIAgentStorageKey` equality is name-based — two keys with the same string name collide regardless of file location
- The no-arg `AIAgentStorage()` constructor was removed; use `AIAgentStorage(serializer)` when constructing one manually
- `toMap()` was removed — iterate via the key set if you need to inspect

`stateManager` (also on `AIAgentContext`) is for agent-lifecycle state, not application data. Don't pile arbitrary data into `stateManager`.

Finish here.

## Step 2 — History Compression

Long agentic runs blow the context window. Compression rules:

- Compress inside a write session
- Compress at deliberate points — end of a phase, start of a subgraph
- Place the call in the boundary node the user identified
- Do not compress at every node

Default to `HistoryCompressionStrategy.WholeHistory` (a single TL;DR) unless the user explicitly asks for last-N, time-window, chunked, or fact-extraction shape. Write the modified node body to disk with an explicit `Path:` label (same convention as `scaffold-agent`):

- `Path: src/main/kotlin/com/example/Strategy.kt` — boundary node with the `replaceHistoryWithTLDR` call (or whichever file defines the strategy / boundary node)

Create the file if it doesn't exist. Do not respond with prose only.

```kotlin
// inside the boundary node body (between exploration and drafting, for example)
llm.writeSession {
    replaceHistoryWithTLDR()   // WholeHistory is the default; pass strategy = HistoryCompressionStrategy.X only when overriding
}
```

Other strategy variants (use only when the user names them — pass via `replaceHistoryWithTLDR(strategy = HistoryCompressionStrategy.X)` and add `import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy`):

- `HistoryCompressionStrategy.NoCompression` — keep everything
- `HistoryCompressionStrategy.WholeHistoryMultipleSystemMessages` — multi-message summary
- `HistoryCompressionStrategy.FromLastNMessages(n)` — keep the last N, drop the rest
- `HistoryCompressionStrategy.FromTimestamp(instant)` — keep messages after timestamp
- `HistoryCompressionStrategy.Chunked(chunkSize)` — chunk-by-chunk summarization
- `HistoryCompressionStrategy.FactRetrieval(concepts)` — extract structured facts about named concepts

`FactRetrieval` was extracted from the removed `AgentMemory` feature in 1.0 — it's now usable standalone in `agents-core`, no memory feature required.

Finish here.

## Step 3 — Cross-Session Memory (`LongTermMemory`)

`AgentMemory` was removed in 1.0. Use `LongTermMemory` for memory that persists across agent runs.

Add the dependency:

```kotlin
implementation("ai.koog:agents-features-longterm-memory:1.2.0-beta")
// for Bedrock AgentCore backend (one option):
implementation("ai.koog:agents-features-longterm-memory-aws:1.2.0-beta")
```

Install the feature inside `AIAgent(...)`'s trailing lambda:

```kotlin
import ai.koog.agents.features.longterm.memory.LongTermMemory
import ai.koog.agents.features.longterm.memory.FailurePolicy

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = "...",
) {
    install(LongTermMemory) {
        searchQueryProvider = ...        // was QueryExtractor pre-1.0
        documentExtractor = ...          // was ExtractionStrategy pre-1.0
        failurePolicy = FailurePolicy.PROPAGATE   // or .SILENTLY_SKIP, .LOG
        // backend-specific config (Bedrock, in-process, etc.)
    }
}
```

**1.0 renames in `LongTermMemory`** (apply when migrating):

- `QueryExtractor` → `SearchQueryProvider`
- `ExtractionStrategy` → `DocumentExtractor`
- `IngestionTiming` was removed — strategies now manage ingestion timing internally

If the user's need is "agent should remember things within one run but not across runs," they don't need `LongTermMemory` — Step 1 (`storage`) covers it.

Finish here.
