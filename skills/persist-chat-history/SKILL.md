---
name: persist-chat-history
description: >
  Persist a Koog 1.3 agent's chat history to a durable backend — JDBC database
  (`chat-history-jdbc`), AWS storage (`chat-history-aws`), or SQL-typed chat memory
  (`chat-memory-sql`) — so conversations survive process restarts and can be retrieved
  by session ID. Distinct from generic agent persistence (state checkpoints) and from
  LongTermMemory (fact retrieval). Use when the user asks to "save chat history",
  "persist conversations", "resume a chat by session", "store chat in Postgres / DynamoDB".
---

# Persist Chat History Skill

Process steps in order. Do not skip ahead.

## Step 1 — Pick the Right Persistence Layer

Three Koog persistence concepts — easy to confuse:

- **Chat history persistence** (this skill) — `chat-history-jdbc`, `chat-history-aws`, `chat-memory-sql`. Stores the conversation messages keyed by session ID. Used to resume a chat where the user left off
- **Generic agent persistence** — checkpoints agent run state (node position, storage, planner state). Used to survive crashes mid-run; covered by `Skill(skill: "add-persistence")`
- **LongTermMemory** — stores extracted *facts* about the user/session for retrieval, not the raw messages; covered by `Skill(skill: "manage-state")`

If the user wants "user picks up the conversation tomorrow", this skill. If "agent resumes a crashed run from where it stopped", invoke `Skill(skill: "add-persistence")`. If "agent remembers things across sessions for retrieval", invoke `Skill(skill: "manage-state")`.

Proceed immediately to Step 2.

## Step 2 — Pick a Backend

Match what the user has available without blocking on a clarifying question:

- **JDBC** (`chat-history-jdbc`) — concrete providers such as `PostgresJdbcChatHistoryProvider` and `MySQLJdbcChatHistoryProvider`. Default when the user names Postgres / MySQL / a JDBC URL
- **AWS** (`chat-history-aws`) — `AgentcoreChatHistoryProvider` for Bedrock AgentCore short-term memory
- **SQL-backed chat history** (`chat-memory-sql`) — Exposed-backed providers such as `PostgresChatHistoryProvider`

These modules supply providers for `ChatMemory`; they are not installable features.

Proceed immediately to Step 3.

## Step 3 — Add the Dependency

```kotlin
implementation("ai.koog:agents-features-chat-history-jdbc:1.3.0")
// or:
// implementation("ai.koog:agents-features-chat-history-aws:1.3.0-beta")
// or:
// implementation("ai.koog:agents-features-chat-memory-sql:1.3.0")
```

For JDBC, also include the driver for the actual database (Postgres, etc.) — that's not provided by Koog.

Proceed immediately to Step 4.

## Step 4 — Install the Feature

Write the modified agent construction, the dependency, and any handler updates to disk with explicit `Path:` labels (same convention as `scaffold-agent`):

- `Path: src/main/kotlin/com/example/Main.kt` — modified agent construction (or the file containing the `AIAgent(...)` call)
- `Path: build.gradle.kts` — appended dependency line
- If the user named a route/handler file (e.g., `Routes.kt`, `ChatController.kt`), write the updated handler under a concrete path using that filename — for example `Path: src/main/kotlin/com/example/Routes.kt`. Never emit a literal angle-bracket placeholder filename

Create files if they don't exist. Do not respond with prose only.

Install in the `AIAgent(...)` trailing lambda. JDBC example:

<!-- compile-example: ChatHistory -->
```kotlin
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.features.chathistory.jdbc.PostgresJdbcChatHistoryProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import org.postgresql.ds.PGSimpleDataSource

suspend fun chatAgent(executor: PromptExecutor): AIAgent<String, String> {
    val dataSource = PGSimpleDataSource().apply {
        setURL(requireNotNull(System.getenv("DB_URL")) { "Set DB_URL" })
        user = requireNotNull(System.getenv("DB_USER")) { "Set DB_USER" }
        password = requireNotNull(System.getenv("DB_PASSWORD")) { "Set DB_PASSWORD" }
    }
    val historyProvider = PostgresJdbcChatHistoryProvider(dataSource)
    historyProvider.migrate()
    return AIAgent(
        promptExecutor = executor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "Help the user resume their conversation.",
    ) {
        install(ChatMemory) {
            chatHistoryProvider = historyProvider
            windowSize(50)
        }
    }
}
```

Include the PostgreSQL JDBC driver for `PGSimpleDataSource`. Migrate the provider's
schema before serving requests. Read credentials from environment variables.

Proceed immediately to Step 5.

## Step 5 — Resume by Session ID

`agent.run(input, sessionId = "...")` accepts a session ID. With chat-history persistence installed:

- A fresh session ID starts a new conversation; the feature writes messages as they happen
- An existing session ID loads prior messages from the backend, then continues — the LLM sees the full history without you having to thread it through

```kotlin
// Day 1
val sessionId = UUID.randomUUID().toString()
agent.run("Hello, I need help triaging a bug", sessionId = sessionId)
// store sessionId somewhere persistent (database, cookie, etc.)

// Day 2
val resumedSessionId = loadSessionIdForUser(...)
agent.run("Following up on the bug from yesterday", sessionId = resumedSessionId)
// agent sees both messages
```

Don't store the session ID inside the agent or in `AIAgentStorage` — that storage is run-scoped. The session ID is the user's identity in the chat history backend; persist it in your application's user/session layer.

Multi-turn within one process: the same agent instance can call `agent.run(input,
sessionId = ...)` repeatedly. `ChatMemory` loads and stores history through its
configured provider. Keep the session ID in the application's user/session layer.

Proceed immediately to Step 6.

## Step 6 — Anti-Pattern: Don't Use Chat-History as a Fact Store

The chat-history feature is for **real conversation turns** between the user and the agent. Do not synthesise fake `Message.User` / `Message.Assistant` entries to inject domain facts the agent "should remember" — even if it's tempting to implement `ChatHistoryProvider` with a hand-rolled list of pseudo-turns.

Symptoms that you've gone down this wrong path:

- You're prepending dates like `[2025-06-19]` to each pseudo-turn to stop the model from treating the latest entry as "today's" task. The data isn't conversation-shaped; the model is correctly confused
- The synthetic `Message.Assistant` content makes claims about actions the agent never actually took
- The data is queryable / filterable (last 90 days, by organizer, by category) but you're forcing it through a sequential message channel

The right Koog primitive depends on the data's shape:

- **Facts the agent should retrieve across sessions** → `LongTermMemory` (see `Skill(skill: "manage-state")` Step 3) — designed for stored facts with explicit `SearchQueryProvider` retrieval
- **Queryable structured data** → expose as a `@Tool` (see `Skill(skill: "add-tool")`) — `getRecentlyUsedFlavors(organizer)`, `getPriorDeclines(eventId)`, etc.
- **Small fixed contextual data** (under ~10 entries, every run) → put it in the `systemPrompt`
- **Run-scoped state seeded in a setup node** → `storage.set(key, value)` (see `Skill(skill: "manage-state")` Step 1)

A custom `ChatHistoryProvider` is legitimate when the source IS conversation messages — replaying a stored chat from an external system, mirroring a Slack thread, etc. It is not legitimate as a backdoor for facts.

Finish here.
