---
name: query-sql-from-agent
description: >
  Expose PostgreSQL analytics lookups to a Koog 1.3 agent as typed JDBC tools.
  Use prepared queries, database-enforced read-only credentials, explicit table
  access, result caps and query timeouts. Use when the user asks to "let the agent
  query the database", "connect my agent to Postgres", "add database access", or
  "expose analytics queries to the LLM". Does not install agents-features-sql;
  that module supplies checkpoint-storage providers, not an SQL-query feature.
---

# Query SQL From Agent Skill

Process steps in order. Do not skip ahead.

## Step 1 — Select the Query Boundary

Default to read-only queries with a database principal granted SELECT only on the
required tables. Inspect the user's actual schema before writing queries.
Use typed lookup tools with prepared SQL and bounded results. Add each required
analytics operation explicitly; do not expose unrestricted model-generated SQL.

`agents-features-sql` provides agent checkpoint storage. It has no installable
`Sql` feature, `schemaScope` configuration or automatically registered query tool.

Proceed immediately to Step 2.

## Step 2 — Add the JDBC Driver

For PostgreSQL, add `implementation("org.postgresql:postgresql:42.7.10")` beside
the existing `ai.koog:koog-agents:1.3.0` dependency. Reuse the application's
DataSource or connection pool when available.

Proceed immediately to Step 3.

## Step 3 — Register Prepared Lookup Tools

Write the tool class and updated agent construction to the user's source files.
The example assumes `public.events(event_type, user_id, occurred_at)` and
`public.users(id)`. Adapt these names to the inspected schema.

<!-- compile-example: SqlLookup -->
```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource
import org.postgresql.ds.PGSimpleDataSource

class AnalyticsQueries(private val dataSource: DataSource) : ToolSet {
    @Tool
    fun countEvents(startInclusive: String, endExclusive: String, limit: Int = 100): List<String> {
        val start = Instant.parse(startInclusive)
        val end = Instant.parse(endExclusive)
        require(end > start) { "End must follow start; use ISO-8601 instants" }
        val rowLimit = limit.coerceIn(1, 100)
        return dataSource.connection.use { connection ->
            connection.isReadOnly = true
            connection.prepareStatement(
                """
                SELECT e.event_type, COUNT(*) AS event_count
                FROM public.events e JOIN public.users u ON u.id = e.user_id
                WHERE e.occurred_at >= ? AND e.occurred_at < ?
                GROUP BY e.event_type ORDER BY e.event_type LIMIT ?
                """.trimIndent(),
            ).use { statement ->
                statement.queryTimeout = 5
                statement.maxRows = rowLimit
                statement.setTimestamp(1, Timestamp.from(start))
                statement.setTimestamp(2, Timestamp.from(end))
                statement.setInt(3, rowLimit)
                statement.executeQuery().use { results ->
                    buildList {
                        while (results.next()) {
                            add("${results.getString(1)}: ${results.getLong(2)}")
                        }
                    }
                }
            }
        }
    }
}

fun analyticsAgent(executor: PromptExecutor) = AIAgent(
    promptExecutor = executor,
    llmModel = OpenAIModels.Chat.GPT4o,
    systemPrompt = """
        Use countEvents for event totals in a requested interval.
        Inputs are ISO-8601 instants; the start is inclusive and the end exclusive.
        The query reads public.events(event_type, user_id, occurred_at) and
        public.users(id). Results are grouped counts, capped at 100 groups.
    """.trimIndent(),
    toolRegistry = ToolRegistry {
        val dataSource = PGSimpleDataSource().apply {
            setURL(requireNotNull(System.getenv("ANALYTICS_DB_URL")) { "Set ANALYTICS_DB_URL" })
            user = requireNotNull(System.getenv("ANALYTICS_DB_USER")) { "Set ANALYTICS_DB_USER" }
            password = requireNotNull(System.getenv("ANALYTICS_DB_PASSWORD")) { "Set ANALYTICS_DB_PASSWORD" }
        }
        tools(AnalyticsQueries(dataSource).asTools())
    },
)
```

Add other approved lookups as typed methods with fixed table/column names and
bound parameters. Register only the operations the user requested. For writes,
use a separate, explicitly authorized tool with transaction and approval controls.

Proceed immediately to Step 4.

## Step 4 — Verify Database Access

Run `./gradlew build`. Against a development database with the intended principal:

- Run the prepared lookup over a known interval and check the expected counts
- Request more than 100 groups and verify the hard result cap
- Confirm that the database principal cannot modify data or read unrelated tables
- Confirm that the agent's tool registry contains `countEvents`

A JDBC read-only flag is a driver hint, not a replacement for database grants.
A row cap bounds returned data, not scan cost; configure server-side statement
limits and indexes for the approved queries. Return only data permitted in the
model's context.

Finish here.
