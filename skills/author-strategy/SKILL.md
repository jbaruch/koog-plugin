---
name: author-strategy
description: >
  Author a custom graph strategy for a Koog 1.2 agent — pick the right node types,
  chain tool execution correctly, build edges with the infix vocabulary, and reach for
  subgraphs (`subgraphWithTask`, `subgraphWithVerification`) when steps deserve their
  own model, prompt, or tool subset while still sharing the agent's message history.
  Use when the user asks to "write a custom strategy", "build a graph for the agent",
  "author a strategy DSL", "add a verify-and-fix loop", "use subgraphs", or describes
  multi-step orchestration that won't fit inside `singleRunStrategy()`. Subgraphs are
  not for context isolation — for an independent agent that does not see the parent's
  history, use `Skill(skill: "add-tool")` Step 3 (sub-agent-as-tool).
---

# Author Strategy Skill

Process steps in order. Do not skip ahead.

## Step 1 — Pick Input/Output Types

Ask the user, one question at a time:

- What's the strategy's input type? `String` (default for chat-shaped agents), `Message.User` (when you control the message shape upstream), or a typed app object?
- What's the output type? `String` is the common default; typed outputs require matching node types

Proceed immediately to Step 2.

## Step 2 — Sketch the Topology Before Writing Code

Before opening the editor, write the topology as a numbered list:

- Each step: what it does, what type it produces
- Each branch: predicate, target
- Each loop: back-edge condition

If you can't name every step and edge in plain English, you don't yet know the strategy — go back and clarify with the user. The DSL captures the topology; if the topology is fuzzy, the DSL output will be too.

Proceed immediately to Step 3.

## Step 3 — Choose Node Variants by Input Type

**This is the most common source of bugs.** Match the node family to its expected input:

- **String-input nodes** (use at the top of a strategy, or after another node returns `String`):
  - `nodeLLMRequest()` — call the LLM with the current message as a user turn
  - `nodeLLMRequestOnlyCallingTools()` — like above but the LLM cannot reply with text, only tool calls
  - `nodeLLMRequestWithoutTools()` — text-only reply
  - `nodeLLMRequestForceOneTool(tool)` — force a specific tool call (covered in `use-llm-node-variants`)
  - `nodeLLMRequestStructured()` — typed JSON output (covered in `add-structured-output`)
  - `nodeLLMRequestMultipleChoices()` / `nodeLLMRequestStreaming()` — covered in `use-llm-node-variants`
  - `nodeLLMModerateText()` — content moderation

- **`Message.User`-input nodes** (use mid-graph where the previous node produced a `Message.User`):
  - `nodeLLMSendMessage()` / variants
  - `nodeLLMSendToolResults()` / variants — pair with `nodeExecuteTools`
  - `nodeLLMModerateMessage()`

Picking the wrong family produces compile-time type errors that read like generic DSL noise. The fix is almost always swapping `Request` for `SendMessage` or vice versa.

Proceed immediately to Step 4.

## Step 4 — Chain `nodeExecuteTools` Explicitly

In 1.0, `nodeExecuteTools()` returns `ReceivedToolResults` directly — the auto-writeback variant from earlier versions is gone. Always:

```kotlin
val nodeCallLLM by nodeLLMRequest()
val nodeExecuteTool by nodeExecuteTools()
val nodeSendToolResult by nodeLLMSendToolResults()

edge(nodeStart forwardTo nodeCallLLM)
edge(nodeCallLLM forwardTo nodeExecuteTool onToolCalls { true })
edge(nodeCallLLM forwardTo nodeFinish onTextMessage { true })
edge(nodeExecuteTool forwardTo nodeSendToolResult)
edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCalls { true })
edge(nodeSendToolResult forwardTo nodeFinish onTextMessage { true })
```

Without the `nodeExecuteTool forwardTo nodeSendToolResult` edge, tool outputs never reach the LLM and the agent silently loses memory of what it ran.

Proceed immediately to Step 5.

## Step 5 — Build Edges with the Infix Vocabulary

Edges declare topology and branching. Vocabulary:

- `edge(a forwardTo b)` — unconditional
- `edge(a forwardTo b onToolCalls { predicate(it) })` — when the LLM produced tool calls
- `edge(a forwardTo b onTextMessage { predicate(it) })` — when the LLM produced a text reply
- `edge(a forwardTo b onCondition { boolean predicate(it) })` — arbitrary predicate
- `edge(a forwardTo b onIsInstance SomeClass::class)` — type-discriminated branching
- `edge(a forwardTo b transformed { reshape(it) })` — change shape between nodes
- `a then b` — shorthand for an unconditional sequential edge

**Member vs extension — which imports you need.** `forwardTo`, `onCondition`, and `transformed` are infix members (no import). The other primitives (`onToolCalls`, `onTextMessage`, `onIsInstance`, `onMessageParts`, `onSuccessful`, `onFailure`, `asUserMessage`, `asToolResultMessage`) are top-level extensions in `ai.koog.agents.core.dsl.extension.*` and need explicit imports. Full table:

```text
skills/author-strategy/references/edge-primitive-imports.md
```

Branching logic belongs in edge predicates, not inside node bodies. Lifting it to edges keeps the topology inspectable.

Proceed immediately to Step 6.

## Step 6 — Lift Steps into Subgraphs

If a step needs several LLM calls, its own tool subset, or its own model, lift it into a subgraph. Reference: `examples/simple-examples/.../subgraphwithtask/CustomStrategy.kt`.

```kotlin
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.agent.subgraphWithVerification
import ai.koog.agents.ext.agent.CriticResult

val generate by subgraphWithTask<Unit, String>(generateTools, llmModel = OpenAIModels.Chat.GPT4o) { input ->
    "system prompt for the generation step..."
}

val verify by subgraphWithVerification(tools = verifyTools, parallelTools = false) { input: String ->
    "system prompt for verifying the generated result..."
}

val fix by subgraphWithTask<CriticResult<String>, String>(tools = fixTools, llmModel = AnthropicModels.Opus_4_6) { vr ->
    "system prompt that incorporates ${vr.feedback}..."
}

edge(verify forwardTo fix onCondition { !it.successful })
edge(verify forwardTo nodeFinish onCondition { it.successful } transformed { "Project is correct." })
edge(fix forwardTo verify)
```

The generate → verify → fix shape is the canonical "real agentic workflow" demo — it shows iterative correction without inventing a planner.

`subgraphWithTask`, `subgraphWithVerification`, and `CriticResult` live in package `ai.koog.agents.ext.agent` but ship inside the **`agents-core`** artifact, which the `koog-agents` umbrella already pulls. No extra dependency needed — the standalone `ai.koog:agents-ext` artifact is a separate `1.0.0-beta` module and is NOT required for these APIs.

If you type-annotate the returned strategy, the strategy type lives in `ai.koog.agents.core.agent.entity.AIAgentGraphStrategy` (not bare `ai.koog.agents.core.agent`).

Proceed immediately to Step 7.

## Step 7 — Name Nodes by Behavior

`val classify by nodeLLMRequest()` reads as `edge(classify forwardTo ...)`. Names like `node1`, `step1`, `llm` erase what the strategy is doing.

Proceed immediately to Step 8.

## Step 8 — Hand Off

- Write the strategy definition and the modified agent construction to disk with explicit `Path:` labels (same convention as `scaffold-agent`):
  - `Path: src/main/kotlin/com/example/Strategy.kt` — strategy definition with the graph DSL
  - `Path: src/main/kotlin/com/example/Main.kt` — modified agent construction passing `strategy = ...`
  - Create files if they don't exist. Do not respond with prose only
- Pass the strategy into `AIAgent(strategy = myStrategy("name"), ...)`. If the strategy isn't `singleRunStrategy` (the default), the `strategy =` parameter must be named explicitly
- Run `./gradlew build` to confirm it compiles. The most common failure is a node-type mismatch — go back to Step 3
- If the strategy needs runtime-decided ordering rather than compile-time topology, this is the wrong primitive — invoke `Skill(skill: "use-planner")` instead

Finish here.
