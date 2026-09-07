---
name: wire-mcp-server
description: >
  Connect a Koog 1.2 agent to an MCP (Model Context Protocol) server, using the
  primary 1.0 Streamable HTTP transport — or fall back to SSE / stdio when the
  remote server doesn't speak Streamable HTTP yet. Adds the agents-mcp dependency,
  builds a ToolRegistry from the MCP server's exposed tools, and merges it with
  the agent's existing tool registry. Use when the user asks to "connect to an
  MCP server", "use the GitHub MCP server", "add MCP tools to my agent", "wire
  Playwright MCP" or similar. Assumes a scaffolded Koog 1.2 project.
---

# Wire MCP Server Skill

Process steps in order. Do not skip ahead.

## Step 1 — Identify the MCP Server

Ask the user, one question at a time:

- The MCP server's transport — Streamable HTTP (recommended), SSE, or stdio (locally launched process)
- The connection details — URL for HTTP/SSE, or the command + args for stdio
- Whether any environment variables (tokens, API keys) need to be set for the server

If the user doesn't know which transport the server supports, default to Streamable HTTP and plan a fallback to SSE if connection fails — the primary 1.0 transport is Streamable HTTP (MCP SDK 0.11.x), but a number of in-the-wild servers still only expose SSE.

Proceed immediately to Step 2.

## Step 2 — Add the Dependency

Open `build.gradle.kts` and confirm the MCP client artifact is in the dependencies block. The umbrella `ai.koog:koog-agents` does not pull MCP — it must be added explicitly. If absent, add it:

```kotlin
implementation("ai.koog:agents-mcp:1.2.0-beta")
```

**Two gotchas the umbrella version hides:**

- `agents-mcp` (and `agents-mcp-server`) ship as **`1.0.0-beta`** — Koog 1.2's stable release did not publish them at `1.0.0`. Pin `1.0.0-beta` (or later when stable).
- They publish only JVM variants. Use the **`-jvm` suffix** (`agents-mcp-jvm`, not bare `agents-mcp`) — without it, Gradle KMP variant resolution can't pick the JVM artifact at this version and the build fails with "could not find" errors.

Re-run `./gradlew --refresh-dependencies` if the project is already imported into the IDE.

Proceed immediately to Step 3.

## Step 3 — Build the MCP `ToolRegistry`

In the file that constructs the agent, build the MCP registry **before** the agent. Pick the form matching the transport.

**Two-import rule for the registry builder:**

- Import `McpToolRegistryProvider` — the `object`
- Import the transport builder by name — `streamableHttp`, `fromSseUrl`, or `fromProcess`
- The builders are top-level extensions in `ai.koog.agents.mcp`, not members of the provider object
- Importing only the provider does NOT bring the builders into scope

**Streamable HTTP (preferred):**

```kotlin
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.streamableHttp

val mcpRegistry = McpToolRegistryProvider.streamableHttp {
    url = "https://<your-mcp-server>/mcp"
    // headers, auth, timeouts — see McpToolRegistryProvider source
}
```

**SSE (fallback):**

```kotlin
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.fromSseUrl

val mcpRegistry = McpToolRegistryProvider.fromSseUrl("https://<your-mcp-server>/sse")
```

**stdio (locally launched):**

```kotlin
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.fromProcess

val process = ProcessBuilder("npx", "-y", "@playwright/mcp@latest").start()
val mcpRegistry = McpToolRegistryProvider.fromProcess(process)
```

This is a suspending call. Run it inside the same `runBlocking { ... }` (or coroutine scope) that constructs the agent.

Proceed immediately to Step 4.

## Step 4 — Merge Into the Agent

If the agent already has a `ToolRegistry`, merge with `+`; if not, pass the MCP registry as the `toolRegistry`:

```kotlin
// merge case
val combined = ToolRegistry { tools(<ExistingTools>().asTools()) } + mcpRegistry

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    toolRegistry = combined,
    systemPrompt = ...,
)
```

Do not mutate the registry after constructing the agent — features wire up during `installFeatures` and read the registry then. Reorder the construction so the registry is final before `AIAgent(...)` runs.

Proceed immediately to Step 5.

## Step 5 — Hand Off

- Run `./gradlew build` to confirm the project still compiles. If it doesn't, the most common cause is a missing import — surface the exact error
- If the MCP server is locally launched, remind the user to set any required environment variables (the values they named in Step 1) before running the agent
- Tell the user one diagnostic check: when the agent runs, the first LLM round-trip should include the MCP tools in its tool schema. If the LLM never picks an MCP tool, the most likely cause is that the MCP server didn't expose the tool. Issue the MCP `tools/list` JSON-RPC method against the server (over its configured transport) to confirm what it actually advertises.

Steps 1–5 covered the client side. If the user is consuming an existing MCP server (the common case), finish here. If the user is **authoring** an MCP server, proceed to Step 6.

## Step 6 — Author an MCP Server (Optional)

Use when the user asks to "expose tools over MCP", "build an MCP server", or "publish a ToolSet as an MCP endpoint". Skip otherwise.

**First read the framing:**

- `@Tool` / `@LLMDescription` / `ToolSet` are LOCAL Koog tool primitives. They bind a Kotlin function to a Koog agent's `ToolRegistry`. No MCP server or client involved
- `startStdioMcpServer` bridges an existing `ToolRegistry` over to MCP. That is the path this step covers
- For projects whose primary goal is publishing tools over MCP without a Koog agent in the picture, use the Kotlin MCP SDK's own server annotation directly. The SDK is `io.modelcontextprotocol:kotlin-sdk`
- The Koog-bridge path below is the right choice when you already have a Koog `ToolRegistry` and want it reachable over MCP too

Add the server module:

```kotlin
implementation("ai.koog:agents-mcp-server:1.2.0-beta")
```

Same `1.0.0-beta` and `-jvm`-suffix gotchas from Step 2 apply.

The same `@Tool` / `@LLMDescription` / `ToolSet` you'd register on a Koog agent via `Skill(skill: "add-tool")` is what you bridge over. Wrap it in a `ToolRegistry` and hand to `startStdioMcpServer`:

```kotlin
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.mcp.server.startStdioMcpServer
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val registry = ToolRegistry { tools(YourTools().asTools()) }  // replace YourTools with the actual ToolSet class
    startStdioMcpServer(registry)
    awaitCancellation()
}
```

The `awaitCancellation()` keeps the process alive while the parent (a Koog agent client via `fromProcess`, or any MCP-compliant client) drives requests over stdio. Write `Path: src/main/kotlin/com/example/McpServer.kt` for the entry point.

Finish here.
