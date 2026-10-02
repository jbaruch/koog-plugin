# Durable checkpoint agents

These Koog 1.3 factories accept an existing graph strategy. Preserve the caller's
planner strategy when adapting the install block to a planner agent. Custom
planners pass non-null state and plan type tokens to `AIAgentPlanner`; their state
and plan types must support serialization. `SimpleLLMPlanner` supplies both tokens.

## PostgreSQL

Add `ai.koog:agents-features-persistence-jdbc:1.3.0` and a PostgreSQL JDBC driver.
Configure the required environment settings documented in the parent skill.

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
                requireNotNull(System.getenv("CHECKPOINT_JDBC_URL")?.takeIf { it.isNotBlank() }) {
                    "Set CHECKPOINT_JDBC_URL to the database JDBC URL; see .env.example"
                },
            )
            user =
                requireNotNull(System.getenv("CHECKPOINT_DB_USER")?.takeIf { it.isNotBlank() }) {
                    "Set CHECKPOINT_DB_USER to a provisioned database role; see .env.example"
                }
            password =
                requireNotNull(System.getenv("CHECKPOINT_DB_PASSWORD")?.takeIf { it.isNotBlank() }) {
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

## Local files

Add `ai.koog:agents-features-snapshot:1.3.0` and choose a persistent directory.

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
