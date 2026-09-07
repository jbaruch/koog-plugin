---
name: wire-acp-server
description: >
  Expose a Koog 1.2 agent through the Agent Client Protocol (ACP) — the lower-level
  bidirectional protocol used by tooling that needs fine-grained control over agent
  invocation lifecycle (cancellation, streaming progress, multi-turn negotiation).
  Use when the user asks to "expose the agent via ACP", "use Agent Client Protocol",
  "wire ACP", or names the `agents-features-acp` module.
---

# Wire ACP Server Skill

Process steps in order. Do not skip ahead.

## Step 1 — Confirm ACP Is the Right Protocol

ACP and A2A are both inter-agent protocols but serve different needs:

- **A2A** (`wire-a2a`) — agent-to-agent calls modeled as RPC. Caller invokes the remote agent, gets a result. Good fit when the remote is another agent doing its own planning
- **ACP** — lower-level bidirectional protocol. Caller and callee negotiate the run; the callee can stream progress, the caller can cancel mid-run, multi-turn handshakes are first-class. Good fit when the *client* is itself tooling that needs fine-grained control (IDE plugins, orchestrators, debuggers)
- **MCP** (`wire-mcp-server`) — tool-host protocol. The remote exposes tools, not agents

If the user's need is "let another agent call this one", redirect to `wire-a2a`. If it's "let a tooling client drive this agent with cancellation and progress events", continue.

Proceed immediately to Step 2.

## Step 2 — Add the Dependency

```kotlin
implementation("ai.koog:agents-features-acp:1.2.0-beta")
```

Proceed immediately to Step 3.

## Step 3 — Install the ACP Feature

Install in the agent's trailing lambda:

```kotlin
import ai.koog.agents.features.acp.ACP

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = "...",
) {
    install(ACP) {
        endpoint = "...."
        // auth, transport, capability flags (cancellation, progress events) — see ACP module
    }
}
```

The feature wires the agent's pipeline into the ACP transport — incoming ACP requests drive the agent through its strategy, outgoing events (LLM round-trips, tool calls, planner steps) surface as ACP progress notifications the client can subscribe to.

Proceed immediately to Step 4.

## Step 4 — Handle Cancellation

ACP clients can cancel mid-run. The feature propagates cancellation as coroutine cancellation through the agent's run — node bodies that catch `CancellationException` and continue will defeat the cancel signal.

If any tools in the registry perform long-running work, ensure they honor `coroutineContext.ensureActive()` checks at safe points. The ACP feature does not patch tool bodies — that's the tool author's responsibility.

Reference example: `examples/acp-agent/` in the repo.

Finish here.
