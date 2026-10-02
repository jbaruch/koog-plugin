---
name: add-persistence
description: >
  Add checkpoint-and-resume to a Koog 1.3 agent. Use Persistence.runFromCheckpoint
  for replay of a supplied checkpoint, or install Persistence with durable storage
  for automatic crash recovery. Use when the user asks to make an agent resumable,
  checkpoint execution, or restart an interrupted workflow.
---

# Add Persistence Skill

This skill is an action router — pick the step that matches the user's intent and execute only that step. Do not run other steps; do not parallelize.

- Saved checkpoint, replay only: Step 1.
- Automatic crash recovery: Step 2.
- Conversation history across visits: invoke `Skill(skill: "persist-chat-history")`.
- Explicit save point and fork: invoke `Skill(skill: "snapshot-and-restore")`.

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
): String =
    Persistence.runFromCheckpoint(
        agent = agent,
        input = "",
        checkpoint = checkpoint,
    )
```

The agent must use the matching graph and node names. Replay resumes **after** the
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
to both. Planner agents retain `ai.koog:agents-planner:1.3.0-beta`. The factory below
demonstrates the graph overload. Write the backend and agent factory to
`src/main/kotlin/com/example/DurableAgent.kt`.

<!-- compile-example: JdbcCheckpointAgent -->
```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.features.persistence.jdbc.PostgresJdbcPersistenceStorageProvider
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import org.postgresql.ds.PGSimpleDataSource

suspend fun jdbcCheckpointAgent(
    executor: PromptExecutor,
    graph: AIAgentGraphStrategy<String, String>,
): AIAgent<String, String> {
    val dataSource =
        PGSimpleDataSource().apply {
            setURL(
                requireNotNull(System.getenv("CHECKPOINT_JDBC_URL")) {
                    "Set CHECKPOINT_JDBC_URL to the database JDBC URL; see .env.example"
                },
            )
            user =
                requireNotNull(System.getenv("CHECKPOINT_DB_USER")) {
                    "Set CHECKPOINT_DB_USER to a provisioned database role; see .env.example"
                }
            password =
                requireNotNull(System.getenv("CHECKPOINT_DB_PASSWORD")) {
                    "Set CHECKPOINT_DB_PASSWORD from the database credential store; see .env.example"
                }
        }
    val provider = PostgresJdbcPersistenceStorageProvider(dataSource)
    provider.migrate()
    return AIAgent(
        promptExecutor = executor,
        llmModel = OpenAIModels.Chat.GPT4o,
        strategy = graph,
    ) {
        install(Persistence) {
            storage = provider
            enableAutomaticPersistence = true
        }
    }
}
```

Write the consumer project's `.env.example` with placeholders and documentation
for these settings, all required when using the PostgreSQL factory:

- `CHECKPOINT_JDBC_URL`: JDBC connection URL from the database service's connection settings.
- `CHECKPOINT_DB_USER`: database role provisioned by the database administrator or service.
- `CHECKPOINT_DB_PASSWORD`: that role's credential from the database administrator or credential store.

Run with a stable work-item session ID: `agent.run(input, sessionId = workItemId)`.
After an interruption, recreate the agent with the same graph, durable provider and
session ID and call `run` again. Persistence restores the latest non-tombstone
checkpoint automatically. Completed runs write a tombstone; the same session then
starts a new run. Explicit replay uses Step 1.

For local disk storage, add `ai.koog:agents-features-snapshot:1.3.0` and use this
factory with a persistent directory, not a temporary directory.

<!-- compile-example: FileCheckpointAgent -->
```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.agents.snapshot.providers.file.JVMFilePersistenceStorageProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import java.nio.file.Path

fun fileCheckpointAgent(
    executor: PromptExecutor,
    graph: AIAgentGraphStrategy<String, String>,
    directory: Path,
): AIAgent<String, String> =
    AIAgent(
        promptExecutor = executor,
        llmModel = OpenAIModels.Chat.GPT4o,
        strategy = graph,
    ) {
        install(Persistence) {
            storage = JVMFilePersistenceStorageProvider(directory)
            enableAutomaticPersistence = true
        }
    }
```

The default storage provider is a no-op. In-memory storage does not survive a
process restart. Automatic graph persistence writes after each nontechnical node;
`enableAutomaticPersistence` is a boolean, not an every-N-steps configuration.
Use explicit save points with automatic persistence disabled for coarser frequency.
Keep node outputs and storage values serializable with the configured serializer;
unserializable outputs can skip a checkpoint. Custom data classes need a supported
serializer, such as `@Serializable` with Kotlinx. Invoke `Skill(skill: "manage-state")`
for typed storage. Checkpoint replay can repeat external side effects; use idempotent
operations or configure rollback tools for the application.

Finish here.
