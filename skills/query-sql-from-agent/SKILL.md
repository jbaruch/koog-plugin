---
name: query-sql-from-agent
description: >
  Give a Koog 1.2 agent the ability to query a SQL database safely — install
  `agents-features-sql`, register the database connection, and expose schema-aware
  query tools the LLM can call. Includes safety guidance (read-only by default,
  schema scoping, parameterized queries). Use when the user asks to "let the agent
  query the database", "add SQL to my agent", "expose a database to the LLM", or
  describes data-retrieval needs the LLM should drive.
---

# Query SQL From Agent Skill

Process steps in order. Do not skip ahead.

## Step 1 — Confirm Read-Only or Read-Write

Ask the user:

- **Read-only** (default recommendation) — the agent can SELECT but cannot INSERT/UPDATE/DELETE/DDL. Safe for analytics and lookup
- **Read-write** — the agent can modify data. Only enable when the user explicitly asks for it AND understands an LLM-driven UPDATE/DELETE can corrupt data on a hallucination

If the user is ambiguous, default to read-only and proceed. Read-write requires explicit confirmation — it's an order of magnitude more dangerous.

Proceed immediately to Step 2.

## Step 2 — Add the Dependency

```kotlin
implementation("ai.koog:agents-features-sql:1.2.0")
```

Also include the JDBC driver for the actual database (Postgres, MySQL, SQLite, etc.) — Koog doesn't bundle drivers.

Proceed immediately to Step 3.

## Step 3 — Configure the Connection

Install in the `AIAgent(...)` trailing lambda:

```kotlin
import ai.koog.agents.features.sql.Sql

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = """
        You can query the production analytics database. Schema is restricted to read-only
        access on the `public.events` and `public.users` tables. Always limit result sets
        to at most 100 rows.
    """.trimIndent(),
) {
    install(Sql) {
        jdbcUrl = System.getenv("ANALYTICS_DB_URL")
        username = System.getenv("ANALYTICS_DB_USER")
        password = System.getenv("ANALYTICS_DB_PASSWORD")

        readOnly = true                                  // STEP 1 default
        schemaScope = listOf("public.events", "public.users")   // narrow what the agent sees
        maxRows = 100                                    // hard cap on result rows per query
    }
}
```

The feature registers `sql_query` (or similar — exact tool name from the module) into the agent's `ToolRegistry` automatically. The LLM calls it like any other tool.

Always read credentials from environment variables.

Proceed immediately to Step 4.

## Step 4 — Pin the Schema in the System Prompt

The LLM needs to know what tables and columns are available before it can write a query. Two options:

- **Inline** — list the schema in the system prompt (works for small schemas; the example above does this implicitly)
- **Discovery tool** — the feature can expose a `describe_schema` tool that returns the schema as JSON. The LLM calls it before writing queries. Use this for larger schemas

Without schema knowledge the LLM hallucinates table/column names and queries fail. Pinning the schema is not optional.

Proceed immediately to Step 5.

## Step 5 — Know the Pitfalls

- **Even read-only is not zero-risk** — a SELECT that scans a billion-row table without an index brings the database down. The `maxRows` cap helps but does not bound query cost. Consider query timeouts at the JDBC layer
- **Never expose a production write user to an agent without a code-level escape valve** — even with `readOnly = true`, the credential itself should be read-only at the database. Defense in depth
- **PII in results lands in the LLM's context** — once a SELECT returns user emails or PII, they're in the conversation history. If the agent has any external surface (web UI, MCP, A2A), assume PII will leak. Filter at the query layer (`SELECT id, COUNT(*) FROM users` not `SELECT * FROM users`)
- **Schema scoping is feature-level, not enforced by the database** — `schemaScope` controls what the *agent* sees, but the database credential grants whatever it grants. The credential's actual permissions are the real boundary

Finish here.
