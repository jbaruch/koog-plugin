package verified

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.snapshot.feature.AgentCheckpointData
import ai.koog.agents.snapshot.feature.isTombstone
import ai.koog.agents.snapshot.providers.file.JVMFilePersistenceStorageProvider
import ai.koog.agents.testing.tools.getMockExecutor
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.serialization.JSONPrimitive
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CheckpointBehaviorTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun explicitForkReusesThePrefixAndChangesContinuationOutput() =
        runBlocking {
            var prefixRuns = 0
            val saved = mutableListOf<AgentCheckpointData>()
            val agent = checkpointForkAgent(getMockExecutor { }, { prefixRuns++ }, { saved.add(it) })
            try {
                assertEquals("result:seed", agent.run("seed", sessionId = "prefix"))
                assertEquals(1, saved.size)
                assertEquals("result:A" to "result:B", compareCheckpointBranches(agent, saved.single()))
                assertEquals(1, prefixRuns)
                assertEquals(1, saved.size)
                assertEquals(JSONPrimitive("seed"), saved.single().graphProperties?.lastOutput)
            } finally {
                agent.close()
            }
        }

    @Test
    fun explicitReplayWorksWithoutInstallingTheWriteSideFeature() =
        runBlocking {
            val saved = mutableListOf<AgentCheckpointData>()
            val writer = checkpointForkAgent(getMockExecutor { }, {}, { saved.add(it) })
            try {
                writer.run("saved-output", sessionId = "writer")
            } finally {
                writer.close()
            }
            var prefixRuns = 0
            val graph =
                strategy<String, String>("checkpoint-fork") {
                    val prepare by node<String, String> { input ->
                        prefixRuns++
                        input
                    }
                    val branchPoint by node<String, String> { it }
                    val continueBranch by node<String, String> { "result:$it" }
                    edge(nodeStart forwardTo prepare)
                    edge(prepare forwardTo branchPoint)
                    edge(branchPoint forwardTo continueBranch)
                    edge(continueBranch forwardTo nodeFinish)
                }
            val reader =
                AIAgent(
                    promptExecutor = getMockExecutor { },
                    llmModel = OpenAIModels.Chat.GPT4o,
                    strategy = graph,
                )
            try {
                assertEquals("result:saved-output", replayCheckpoint(reader, saved.single()))
                assertEquals(0, prefixRuns)
            } finally {
                reader.close()
            }
        }

    @Test
    fun recreatedFileProviderResumesInterruptedRunAndCompletedRunStartsFresh() =
        runBlocking {
            var prefixRuns = 0
            var interrupt = true
            val graph =
                strategy<String, String>("durable-checkpoint") {
                    val prepare by node<String, String> { input ->
                        prefixRuns++
                        "$input-prepared"
                    }
                    val complete by node<String, String> { input ->
                        check(!interrupt) { "Simulated interruption" }
                        input
                    }
                    edge(nodeStart forwardTo prepare)
                    edge(prepare forwardTo complete)
                    edge(complete forwardTo nodeFinish)
                }
            val first = fileCheckpointAgent(getMockExecutor { }, graph, directory)
            try {
                assertFailsWith<IllegalStateException> { first.run("original", sessionId = "work-item") }
            } finally {
                first.close()
            }
            assertEquals(1, prefixRuns)
            interrupt = false
            val restarted = fileCheckpointAgent(getMockExecutor { }, graph, directory)
            try {
                assertEquals("original-prepared", restarted.run("replacement", sessionId = "work-item"))
                assertEquals(1, prefixRuns)
                val provider = JVMFilePersistenceStorageProvider(directory)
                assertTrue(requireNotNull(provider.getLatestCheckpoint("work-item")).isTombstone())
                assertEquals("fresh-prepared", restarted.run("fresh", sessionId = "work-item"))
                assertEquals(2, prefixRuns)
            } finally {
                restarted.close()
            }
        }
}
