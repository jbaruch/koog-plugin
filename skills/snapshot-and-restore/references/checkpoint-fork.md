# Manual checkpoint forks

Add `ai.koog:agents-features-snapshot:1.3.0`. Adapt the prefix and continuation
nodes to the existing graph. This in-memory example compares two continuations
from one saved checkpoint without repeating the prefix. Use durable storage for
recovery in a later process.

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
                onCheckpoint(
                    requireNotNull(checkpoint) {
                        "Register a supported serializer and ensure node output and stored values are serializable"
                    },
                )
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
    val graphState =
        requireNotNull(checkpoint.graphProperties) {
            "Supply a checkpoint from the matching graph strategy; planner checkpoints cannot be forked here"
        }
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
