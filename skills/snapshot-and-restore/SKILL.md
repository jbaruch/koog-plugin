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

Add `ai.koog:agents-features-snapshot:1.3.0` to `build.gradle.kts`. Write the following
to `src/main/kotlin/com/example/CheckpointFork.kt`, adapting the named prefix and
continuation nodes to the developer's existing graph.

<!-- compile-example: CheckpointFork -->
```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.execution.path
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.snapshot.feature.AgentCheckpointData
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.agents.snapshot.feature.withPersistence
import ai.koog.agents.snapshot.providers.InMemoryPersistenceStorageProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.serialization.JSONPrimitive
import ai.koog.serialization.typeToken

fun checkpointForkAgent(
    executor: PromptExecutor,
    onPrefix: () -> Unit,
    onCheckpoint: (AgentCheckpointData) -> Unit,
): AIAgent<String, String> {
    val graph =
        strategy<String, String>("checkpoint-fork") {
            val prepare by node<String, String> { input ->
                onPrefix()
                input
            }
            val branchPoint by node<String, String> { input ->
                val checkpoint =
                    withPersistence { context ->
                        createCheckpointAfterNode(
                            agentContext = context,
                            nodePath = context.executionInfo.path(),
                            lastOutput = input,
                            lastOutputType = typeToken<String>(),
                            version = 0L,
                        )
                    }
                onCheckpoint(requireNotNull(checkpoint) { "Checkpoint serialization failed" })
                input
            }
            val continueBranch by node<String, String> { input -> "result:$input" }
            edge(nodeStart forwardTo prepare)
            edge(prepare forwardTo branchPoint)
            edge(branchPoint forwardTo continueBranch)
            edge(continueBranch forwardTo nodeFinish)
        }
    return AIAgent(
        promptExecutor = executor,
        llmModel = OpenAIModels.Chat.GPT4o,
        strategy = graph,
    ) {
        install(Persistence) {
            storage = InMemoryPersistenceStorageProvider()
            enableAutomaticPersistence = false
        }
    }
}

suspend fun compareCheckpointBranches(
    agent: AIAgent<String, String>,
    checkpoint: AgentCheckpointData,
): Pair<String, String> {
    val graphState = requireNotNull(checkpoint.graphProperties)
    suspend fun branch(variant: String): String =
        Persistence.runFromCheckpoint(
            agent = agent,
            input = "",
            checkpoint =
                AgentCheckpointData(
                    checkpointId = checkpoint.checkpointId,
                    createdAt = checkpoint.createdAt,
                    messageHistory = checkpoint.messageHistory,
                    llmParams = checkpoint.llmParams,
                    version = checkpoint.version,
                    graphProperties = graphState.copy(lastOutput = JSONPrimitive(variant)),
                    properties = checkpoint.properties,
                    llmModel = checkpoint.llmModel,
                    tools = checkpoint.tools,
                    storage = checkpoint.storage,
                    agentIterations = checkpoint.agentIterations,
                ),
            sessionId = "fork-$variant",
        )
    return branch("A") to branch("B")
}
```

Run the initial prefix once and retain the `AgentCheckpointData` supplied to
`onCheckpoint`. Pass it to `compareCheckpointBranches`. Replay resumes after the
saved node. The two copies replace its serialized output for the continuation;
changing the ordinary `input` argument alone does not change the restored output.
Use distinct session IDs for independent branches and stable matching graph/node
names. Forks preserve the saved prefix history and typed storage.

In-memory storage is suitable for same-process comparison. For later process
restarts, replace it with a durable provider and retrieve the saved checkpoint by
session and checkpoint ID. Increment checkpoint versions for repeated saves in a
session. Node outputs and typed storage must serialize successfully; custom data
classes require a supported serializer. Invoke `Skill(skill: "manage-state")` for
typed storage. Replay does not undo external side effects; isolate branch effects
or make them idempotent.

Finish here.
