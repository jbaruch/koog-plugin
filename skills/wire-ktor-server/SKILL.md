---
name: wire-ktor-server
description: >
  Expose a Koog 1.2 agent through a Ktor server — install the `koog-ktor` plugin,
  load agent configuration from `application.conf` / `.yaml`, and register MCP servers
  inside the plugin block. Use when the user asks to "expose the agent over HTTP",
  "add Koog to my Ktor app", "wire the Ktor plugin", or describes a server-shaped
  deployment.
---

# Wire Ktor Server Skill

Process steps in order. Do not skip ahead.

## Step 1 — Add the Dependency

```kotlin
implementation("ai.koog:koog-ktor:1.2.0-beta")
```

The umbrella `koog-agents` does not include the Ktor integration — add it explicitly.

Proceed immediately to Step 2.

## Step 2 — Install the Plugin

In the `Application` module, install the `Koog` plugin. The minimal install is just the prompt-executor wiring:

```kotlin
import ai.koog.ktor.Koog
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import io.ktor.server.application.*

fun Application.module() {
    install(Koog) {
        promptExecutor(simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")))
    }
}
```

`install(Koog) { ... }` is the entry point. The plugin's companion implements Ktor's `Plugin<ApplicationCallPipeline, KoogAgentsConfig, Koog>` so it composes with other Ktor plugins (`ContentNegotiation`, `Authentication`, `CallLogging`) the usual way.

Steps 3 and 4 are conditional add-ons within the sequential flow:

- Step 3 — execute when the user named MCP servers; otherwise pass through as a no-op
- Step 4 — execute when the user named HOCON or YAML external config; otherwise pass through as a no-op

Proceed immediately to Step 3.

## Step 3 — Register MCP Servers Inside the Plugin (Optional)

Skip this step entirely if the user did not name MCP servers (no `mcp { ... }` block needed in the install). Proceed to Step 4 either way.

The MCP example below uses `HttpClient(CIO)`. Add `implementation("io.ktor:ktor-client-cio:<ktor-version>")` to `build.gradle.kts` if the project does not already pull a Ktor client engine (server-only projects don't).

The Ktor plugin exposes a typed `mcp { ... }` block that registers MCP servers as part of the plugin configuration — no separate `McpToolRegistryProvider` plumbing in user code:

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

val httpClient = HttpClient(CIO)

install(Koog) {
    mcp {
        // streamable HTTP (1.0 primary transport)
        streamableHttp(url = "https://mcp.github.example/v1", httpClient = httpClient)

        // SSE fallback
        sse(url = "https://legacy-mcp.example/sse")

        // stdio (locally launched)
        process(ProcessBuilder("npx", "-y", "@playwright/mcp@latest").start())
    }
}
```

Use Streamable HTTP unless the remote server only supports SSE. For the standalone (non-Ktor) form, invoke `Skill(skill: "wire-mcp-server")`.

Proceed immediately to Step 4.

## Step 4 — Load Config from `application.conf` (Optional — skip unless user names HOCON/YAML config)

Skip this step entirely if the user did not name HOCON or YAML external config. Proceed to Step 5 either way.

For deployments where agent shape should be configuration, not code, `KoogAgentsConfig` reads `application.conf` (HOCON) or `application.yaml` via `loadAgentsConfig()`:

```hocon
koog {
  agents {
    name = "github-assistant"
    model.id = "gemini-2.5-flash-lite"
    model.options.temperature = 1.0
    system_prompt = """
      You are a helpful GitHub assistant.
    """
    tools = [
      {
        type = "MCP"
        id = "GitHub"
        options.url = "https://mcp.github.example/v1"
      }
    ]
  }
}
```

The agent is then resolvable from the Ktor application call via a typed accessor — the plugin's call-attribute key is `Koog.key`. Read it inside route handlers when you need the configured agent.

Proceed immediately to Step 5.

## Step 5 — Add a Route

Expose the agent on an HTTP route. A minimal POST endpoint:

```kotlin
routing {
    post("/agent") {
        val input = call.receiveText()
        val agent = call.attributes[Koog.key].agent  // resolution via plugin attribute key
        val result = agent.run(input)
        call.respondText(result)
    }
}
```

For streaming responses, use `nodeLLMRequestStreaming` inside a custom strategy (see `use-llm-node-variants`) and Ktor's `respondBytesWriter { ... }` or SSE/WebSocket transport.

Write the modified module and the dependency to disk with explicit `Path:` labels (same convention as `scaffold-agent`):

- `Path: src/main/kotlin/com/example/Application.kt` — the file containing `Application.module()` (rename to the user's actual filename if different)
- `Path: build.gradle.kts` — appended `ai.koog:koog-ktor:1.2.0-beta` dependency line

Create files if they don't exist. Do not respond with prose only. Finish here.
