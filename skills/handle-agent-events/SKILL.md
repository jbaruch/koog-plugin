---
name: handle-agent-events
description: >
  Install per-step event handlers on a Koog 1.2 agent — tool-call start/end, LLM
  request/response, agent finish, error events. Useful for stdout logging during
  development, visualizing planner decisions on stage during demos, or pushing
  events to a non-OTel sink. Use when the user asks to "log tool calls", "see what
  the agent is doing", "add event handlers", "visualize the planner", "trace each
  step" — anything where the goal is human-readable per-step output, not production
  metrics.
---

# Handle Agent Events Skill

Process steps in order. Do not skip ahead.

## Step 1 — Add the Dependency

```kotlin
implementation("ai.koog:agents-features-event-handler:1.2.0")
```

The umbrella `koog-agents` does not include the event handler — add it explicitly.

Proceed immediately to Step 2.

## Step 2 — Install Inside the Trailing Lambda

Write the modified agent construction and the dependency to disk with explicit `Path:` labels (same convention as `scaffold-agent`):

- `Path: src/main/kotlin/com/example/Main.kt` — modified agent construction (or whichever file contains the `AIAgent(...)` call)
- `Path: build.gradle.kts` — appended dependency line

Create files if they don't exist. Do not respond with prose only.

```kotlin
import ai.koog.agents.features.eventhandler.handleEvents

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = "...",
) {
    handleEvents {
        onToolCallStarting { ctx ->
            println("→ Tool '${ctx.toolName}' called with args: ${ctx.toolArgs}")
        }
        onToolCallFinished { ctx ->
            println("← Tool '${ctx.toolName}' returned: ${ctx.result}")
        }
        onLLMRequestStarting { ctx ->
            println("LLM ▶ ${ctx.model}")
        }
        onLLMRequestFinished { ctx ->
            println("LLM ◀ ${ctx.tokenUsage}")
        }
        onAgentFinished { ctx ->
            println("✓ Agent finished after ${ctx.iterations} iterations")
        }
        onAgentError { ctx ->
            System.err.println("✗ Agent error: ${ctx.error}")
        }
    }
}
```

Each callback receives a typed context object with what the event carried. Don't allocate inside the hot path — for high-throughput agents, an event handler that synchronously logs every tool call to stdout will dominate the run.

Proceed immediately to Step 3.

## Step 3 — Don't Conflate With OpenTelemetry

Event handlers and OpenTelemetry coexist; they don't replace each other:

- **Event handlers** are for human-readable per-step output. Stdout logging during development; conference-demo visualization (planner step count ticking up live on a side panel); ad-hoc push to a non-OTel sink (Slack, a webhook, a custom database)
- **OpenTelemetry** is for production signal. Aggregated metrics (`gen_ai.client.token.usage`, `gen_ai.client.tool.count`), distributed traces, dashboards

If the user wants production observability, install both — the event handler for the development surface, OpenTelemetry for the production signal. See `add-observability` for the OTel install.

If the user only wants demo-time visualization, the event handler alone is enough — no OTel collector to stand up.

Finish here.
