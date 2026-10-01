package verified

import ai.koog.agents.cli.transport.CliEvent
import ai.koog.agents.cli.transport.CliTransport
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.AIAgentStorage
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.subgraph
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.agents.testing.tools.getMockExecutor
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.rag.base.files.JVMFileSystemProvider
import ai.koog.serialization.kotlinx.KotlinxSerializer
import ai.koog.skills.discovery.discoverSkills
import ai.koog.skills.prompt.SkillsPromptFormat
import ai.koog.skills.prompt.generateSkillsPrompt
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Timestamp
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class BehaviorTest {
    @TempDir
    lateinit var directory: Path

    private fun addSkill(
        root: Path,
        name: String,
    ) {
        val skill = Files.createDirectories(root.resolve(name))
        Files.writeString(skill.resolve("SKILL.md"), "---\nname: $name\ndescription: Test skill\n---\nDo the task.\n")
    }

    @Test
    fun storageRoundTripsThePublishedKey() =
        runBlocking {
            val storage = AIAgentStorage(KotlinxSerializer())
            assertEquals("triage", rememberNode(storage, "triage"))
            val restored = AIAgentStorage(KotlinxSerializer())
            restored.putAllSerialized(storage.toSerializedMap())
            assertEquals("triage", restored.get(currentNodeKey))
        }

    @Test
    fun functionalStrategyUsesTheRequestAndReturnsTrimmedText() =
        runBlocking {
            val executor =
                getMockExecutor {
                    mockLLMAnswer("  This is unsatisfactory.  ") onRequestEquals "yo this kinda sucks"
                }
            try {
                assertEquals("This is unsatisfactory.", rewrite(executor, "yo this kinda sucks"))
            } finally {
                executor.close()
            }
        }

    private suspend fun historySeenByPhase(isolated: Boolean): String {
        val graph =
            strategy<String, String>("history-scope") {
                val seed by node<String, String> { input ->
                    addAccountContext(this, "persistent account context")
                    llm.writeSession { appendPrompt { assistant("prior phase context") } }
                    input
                }
                val phase by subgraph<String, String>(freshHistory = isolated) {
                    val inspect by node<String, String> {
                        llm.readSession { prompt.messages.joinToString("|") { it.toString() } }
                    }
                    edge(nodeStart forwardTo inspect)
                    edge(inspect forwardTo nodeFinish)
                }
                edge(nodeStart forwardTo seed)
                edge(seed forwardTo phase)
                edge(phase forwardTo nodeFinish)
            }
        val agent =
            AIAgent(
                promptExecutor = getMockExecutor { },
                llmModel = OpenAIModels.Chat.GPT4o,
                strategy = graph,
                systemPrompt = "Test history scope.",
            )
        return try {
            agent.run("typed input")
        } finally {
            agent.close()
        }
    }

    @Test
    fun sharedSubgraphSeesPriorPhaseMessages() =
        runBlocking {
            assertTrue(historySeenByPhase(isolated = false).contains("prior phase context"))
        }

    @Test
    fun freshHistorySubgraphDoesNotSeePriorPhaseMessages() =
        runBlocking {
            assertFalse(historySeenByPhase(isolated = true).contains("prior phase context"))
        }

    @Test
    fun freshHistoryPreservesSystemContext() =
        runBlocking {
            assertTrue(historySeenByPhase(isolated = true).contains("persistent account context"))
        }

    @Test
    fun preparedLookupCapsRowsAndFiltersTheInterval() {
        val dataSource =
            JdbcDataSource().apply {
                setURL("jdbc:h2:file:" + directory.resolve("analytics"))
            }
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE public.users(id BIGINT PRIMARY KEY)")
                statement.execute("CREATE TABLE public.events(event_type VARCHAR(32), user_id BIGINT, occurred_at TIMESTAMP)")
                statement.execute("INSERT INTO public.users VALUES (1)")
            }
            connection.prepareStatement("INSERT INTO public.events VALUES (?, 1, ?)").use { statement ->
                repeat(150) { index ->
                    statement.setString(1, "event-$index")
                    statement.setTimestamp(2, Timestamp.from(Instant.parse("2025-01-01T12:00:00Z")))
                    statement.addBatch()
                }
                statement.setString(1, "outside-interval")
                statement.setTimestamp(2, Timestamp.from(Instant.parse("2025-01-03T12:00:00Z")))
                statement.addBatch()
                statement.executeBatch()
            }
        }
        val tools = AnalyticsQueries(dataSource)
        val results = tools.countEvents("2025-01-01T00:00:00Z", "2025-01-02T00:00:00Z", limit = 1000)
        assertEquals(100, results.size)
        assertFalse(results.any { it.contains("outside-interval") })
        assertTrue(results.all { it.endsWith(": 1") })
        assertEquals(2, tools.countEvents("2025-01-01T00:00:00Z", "2025-01-02T00:00:00Z", limit = 2).size)
    }

    @Test
    fun invalidLookupIntervalFailsBeforeConnecting() {
        val tools = AnalyticsQueries(JdbcDataSource())
        assertFailsWith<IllegalArgumentException> {
            tools.countEvents("2025-01-02T00:00:00Z", "2025-01-01T00:00:00Z")
        }
    }

    @Test
    fun discoverySnapshotDoesNotRefreshUntilRediscovery() =
        runBlocking {
            val root = Files.createDirectory(directory.resolve("skills"))
            addSkill(root, "first")
            val discovered = discoverSkills(JVMFileSystemProvider.ReadOnly, listOf(root.toString()))
            val originalPrompt = generateSkillsPrompt(discovered, SkillsPromptFormat.XML)
            addSkill(root, "second")
            assertEquals(listOf("first"), discovered.map { it.name })
            assertFalse(originalPrompt.contains("second"))
            val refreshed = discoverSkills(JVMFileSystemProvider.ReadOnly, listOf(root.toString()))
            assertEquals(setOf("first", "second"), refreshed.map { it.name }.toSet())
            assertTrue(generateSkillsPrompt(refreshed, SkillsPromptFormat.XML).contains("second"))
        }

    @Test
    fun defaultReadOnlyToolCanReadOutsideTheDiscoveryRoot() =
        runBlocking {
            val root = Files.createDirectory(directory.resolve("skills"))
            addSkill(root, "first")
            discoverSkills(JVMFileSystemProvider.ReadOnly, listOf(root.toString()))
            val outside = Files.writeString(directory.resolve("outside.txt"), "synthetic outside content")
            val result = ReadFileTool(JVMFileSystemProvider.ReadOnly).execute(ReadFileTool.Args(outside.toString()))
            assertTrue(result.toString().contains("synthetic outside content"))
        }

    @Test
    fun confinedToolsReadAndListInsideTheRoot() {
        val root = Files.createDirectory(directory.resolve("skills"))
        val inside = Files.writeString(root.resolve("SKILL.md"), "synthetic inside content")
        val tools = ConfinedSkillFiles(root)
        assertEquals("synthetic inside content", tools.readSkill(inside.toString()))
        assertEquals(listOf(inside.toRealPath().toString()), tools.listSkills(root.toString()))
    }

    @Test
    fun confinedToolsRejectTraversalAndAbsoluteOutsidePaths() {
        val root = Files.createDirectory(directory.resolve("skills"))
        val outside = Files.writeString(directory.resolve("outside.txt"), "synthetic outside content")
        val tools = ConfinedSkillFiles(root)
        assertFailsWith<IllegalArgumentException> { tools.readSkill(outside.toString()) }
        assertFailsWith<IllegalArgumentException> { tools.readSkill(root.resolve("../outside.txt").toString()) }
        assertFailsWith<IllegalArgumentException> { tools.listSkills(directory.toString()) }
    }

    @Test
    fun confinedToolsRejectSiblingPrefixAndSymlinkEscapes() {
        val root = Files.createDirectory(directory.resolve("skills"))
        val sibling = Files.createDirectory(directory.resolve("skills-other"))
        val outside = Files.writeString(sibling.resolve("outside.txt"), "synthetic outside content")
        val link = Files.createSymbolicLink(root.resolve("escape.txt"), outside)
        val tools = ConfinedSkillFiles(root)
        assertFailsWith<IllegalArgumentException> { tools.readSkill(outside.toString()) }
        assertFailsWith<IllegalArgumentException> { tools.readSkill(link.toString()) }
        assertFailsWith<IllegalArgumentException> { tools.listSkills(root.toString()) }
    }

    private fun probeEnvironment(withBillingVariables: Boolean): String {
        val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        val process = ProcessBuilder(java, "-cp", System.getProperty("probe.classpath"), "verified.EnvironmentProbe")
        process.environment().clear()
        if (withBillingVariables) {
            for (name in listOf("ANTHROPIC_API_KEY", "CODEX_API_KEY", "OPENAI_API_KEY")) {
                process.environment()[name] = "synthetic-test-value"
            }
        }
        process.directory(directory.toFile())
        val child = process.start()
        val output =
            child.inputStream
                .bufferedReader()
                .readText()
                .trim()
        val errors = child.errorStream.bufferedReader().readText()
        assertEquals(0, child.waitFor(), errors)
        return output.lineSequence().single { it.startsWith("billing=") }.removePrefix("billing=")
    }

    @Test
    fun emptyCliOverridesPreserveInheritedBillingVariables() {
        assertEquals("true,true,true", probeEnvironment(withBillingVariables = true))
    }

    @Test
    fun sanitizedParentRemovesBillingVariablesFromCliChildren() {
        assertEquals("false,false,false", probeEnvironment(withBillingVariables = false))
    }
}

object EnvironmentProbe {
    @JvmStatic
    fun main(args: Array<String>) =
        runBlocking {
            val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
            val events =
                CliTransport
                    .default()
                    .execute(
                        command = listOf(java, "-cp", System.getProperty("java.class.path"), "verified.EnvironmentReporter"),
                        workspace = Path.of(".").toAbsolutePath().toString(),
                        env = emptyMap(),
                        timeout = 10.seconds,
                    ).toList()
            check(events.none { it is CliEvent.Failed }) { "CLI probe failed" }
            check(events.filterIsInstance<CliEvent.Exit>().single().code == 0) { "CLI probe exited nonzero" }
            println("billing=" + events.filterIsInstance<CliEvent.Stdout>().single().content)
        }
}

object EnvironmentReporter {
    @JvmStatic
    fun main(args: Array<String>) {
        println(
            listOf("ANTHROPIC_API_KEY", "CODEX_API_KEY", "OPENAI_API_KEY")
                .joinToString(",") { System.getenv(it).let { value -> !value.isNullOrEmpty() }.toString() },
        )
    }
}
