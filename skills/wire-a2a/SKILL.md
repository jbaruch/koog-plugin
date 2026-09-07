---
name: wire-a2a
description: >
  Wire the Agent-to-Agent (A2A) protocol — expose a Koog 1.2 agent as an A2A server,
  or consume a remote A2A server as a client (typically to make a remote agent
  callable as a tool by a local agent). Use when the user asks to "expose the
  agent via A2A", "use A2A protocol", "call a remote agent", "register an A2A
  client", or names any of `a2a-server`, `a2a-client`.
---

# Wire A2A Skill

This skill is an action router — pick the step that matches the user's intent and execute only that step. Do not run other steps; do not parallelize.

Available actions:

- **Step 1** — Serve: expose a local agent as an A2A endpoint
- **Step 2** — Consume: call a remote A2A server from a local agent

## Step 1 — Serve a Local Agent via A2A

Add the dependencies:

```kotlin
implementation("ai.koog:a2a-core:1.2.0-beta")
implementation("ai.koog:a2a-server:1.2.0-beta")
```

Construct the agent normally, then wrap it in an A2A server:

```kotlin
import ai.koog.a2a.server.A2AServer

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = "...",
)

val server = A2AServer.builder()
    .agent(agent)
    .port(8080)
    .build()

server.start()
```

The server exposes the agent over the A2A wire protocol — remote clients can invoke `run(...)` on it as if it were a local agent. Auth, transport, and routing options live on the builder.

For Ktor-backed deployments, the A2A server can also install as a Ktor route — wire the route inside an `install(Koog) { ... }` block (see `wire-ktor-server`) rather than running a separate server process.

Finish here.

## Step 2 — Consume a Remote A2A Server

Add the client dependency:

```kotlin
implementation("ai.koog:a2a-core:1.2.0-beta")
implementation("ai.koog:a2a-client:1.2.0-beta")
```

Build a client and use it inside the local agent — typically wrapped as a tool, so the local LLM can decide when to delegate to the remote agent:

```kotlin
import ai.koog.a2a.client.A2AClient
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTools

val remoteClient = A2AClient.builder()
    .endpoint("https://remote-agent.example/a2a")
    .auth(...)
    .build()

@LLMDescription("Tools that delegate to a remote specialist agent")
class RemoteAgentTools(private val client: A2AClient) : ToolSet {
    @Tool
    @LLMDescription("Ask the specialist agent for a deep analysis of the input")
    suspend fun deepAnalysis(@LLMDescription("Input to analyze") input: String): String =
        client.run(input)
}

val localAgent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    toolRegistry = ToolRegistry { tools(RemoteAgentTools(remoteClient).asTools()) },
    systemPrompt = "...",
)
```

A2A is similar in spirit to MCP — both expose remote capabilities — but the wire shapes differ. Use A2A when the remote is itself a full agent (it plans, retries, calls its own tools); use MCP when the remote is a tool host. See `wire-mcp-server` for the MCP path.

Finish here.
