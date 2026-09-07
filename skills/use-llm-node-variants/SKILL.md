---
name: use-llm-node-variants
description: >
  Use a non-default LLM node variant inside a Koog 1.2 strategy — streaming output,
  multiple-choice sampling, content moderation, or forcing a specific tool call. Use
  when the user asks for "streaming", "multiple completions / sampling", "moderation",
  "force one tool", "force the LLM to call a specific tool", or names any of
  `nodeLLMRequestStreaming`, `nodeLLMRequestMultipleChoices`, `nodeLLMModerateText`,
  `nodeLLMRequestForceOneTool`.
---

# Use LLM Node Variants Skill

This skill is an action router — pick the step that matches the user's intent and execute only that step. Do not run other steps; do not parallelize.

Available actions:

- **Step 1** — Streaming output (`nodeLLMRequestStreaming`)
- **Step 2** — Multiple-choice sampling (`nodeLLMRequestMultipleChoices`)
- **Step 3** — Content moderation (`nodeLLMModerateText` / `nodeLLMModerateMessage`)
- **Step 4** — Force a specific tool call (`nodeLLMRequestForceOneTool`)

## Step 1 — Streaming Output

Use when the caller needs to see partial output as it arrives (Ktor server endpoint, CLI with live print, conference demo with text appearing live).

Write the strategy and the modified agent construction to disk with explicit `Path:` labels (same convention as `scaffold-agent`):

- `Path: src/main/kotlin/com/example/Strategy.kt` — strategy with the streaming node
- `Path: src/main/kotlin/com/example/Main.kt` — modified agent construction passing `strategy = ...`

Create files if they don't exist. Do not respond with prose only.

```kotlin
val strategy = strategy<String, String>("stream-reply") {
    val streamReply by nodeLLMRequestStreaming { chunk ->
        // chunk-handler — runs on every streamed chunk
        print(chunk.text)
    }

    edge(nodeStart forwardTo streamReply)
    edge(streamReply forwardTo nodeFinish)
}
```

The chunk-handler lambda runs synchronously on every streamed delta. Don't block inside it — the LLM's stream stalls if the handler is slow. For network I/O (writing chunks to a Ktor response), use a channel/flow downstream rather than calling `respond*` directly inside the handler.

Streaming doesn't compose with tool calls in the same node — for "stream the reply text but still allow tool calls", use a `singleRunStrategy` shape and stream from `nodeLLMRequestStreaming` only on the text-reply branch.

Finish here.

## Step 2 — Multiple-Choice Sampling

Use when you want multiple completions for the same prompt and downstream code (or a critic node) picks the best.

Write the strategy and modified agent construction to disk via `Path:`:

- `Path: src/main/kotlin/com/example/Strategy.kt` — strategy with the sampling + picker nodes
- `Path: src/main/kotlin/com/example/Main.kt` — modified agent construction

Create files if missing. Do not respond with prose only.

```kotlin
val strategy = strategy<String, String>("sample-and-pick") {
    val sample by nodeLLMRequestMultipleChoices(numChoices = 3)
    val pick by node<List<Message.User>, Message.User>("pick-best") { choices ->
        // pick logic — e.g., longest reply, or run a critic LLM
        choices.maxBy { it.content.length }
    }

    edge(nodeStart forwardTo sample)
    edge(sample forwardTo pick)
    edge(pick forwardTo nodeFinish transformed { it.content })
}
```

Each choice costs one LLM completion at full token rates — sample 3 for 3× the cost. Use when the picker's signal is cheaper or more reliable than the per-completion quality.

Finish here.

## Step 3 — Content Moderation

Two variants:

- `nodeLLMModerateText()` — String input
- `nodeLLMModerateMessage()` — `Message.User` input

Both call a moderation classifier (provider-dependent) and produce a moderation result that downstream edges branch on.

Write the strategy and modified agent construction to disk via `Path:`:

- `Path: src/main/kotlin/com/example/Strategy.kt` — moderated strategy
- `Path: src/main/kotlin/com/example/Main.kt` — modified agent construction

Create files if missing. Do not respond with prose only.

```kotlin
val strategy = strategy<String, String>("moderated-chat") {
    val moderate by nodeLLMModerateText()
    val safeReply by nodeLLMRequest()

    edge(nodeStart forwardTo moderate)
    edge(moderate forwardTo nodeFinish onCondition { it.flagged } transformed { "I can't help with that request." })
    edge(moderate forwardTo safeReply onCondition { !it.flagged })
    edge(safeReply forwardTo nodeFinish onTextMessage { true })
}
```

The moderation node returns a typed result with a `flagged` boolean and category breakdown — branch on `flagged` to short-circuit, and on categories if you need finer-grained handling.

Finish here.

## Step 4 — Force a Specific Tool Call

Use when the next step must invoke a specific tool — no LLM choice, no fallback to text. Common case: a "router" agent where the first step always calls `classify_intent`.

Write the strategy and modified agent construction to disk via `Path:`:

- `Path: src/main/kotlin/com/example/Strategy.kt` — strategy with the forced-tool node
- `Path: src/main/kotlin/com/example/Main.kt` — modified agent construction

Create files if missing. Do not respond with prose only.

```kotlin
val classifyTool = ClassifyIntentTool()

val strategy = strategy<String, String>("force-router") {
    val forceClassify by nodeLLMRequestForceOneTool(classifyTool)
    val execute by nodeExecuteTools()
    val followUp by nodeLLMSendToolResults()

    edge(nodeStart forwardTo forceClassify)
    edge(forceClassify forwardTo execute)
    edge(execute forwardTo followUp)
    edge(followUp forwardTo nodeFinish onTextMessage { true })
}
```

The forced tool must already be in the agent's `ToolRegistry`. Forcing a tool not in the registry is a runtime error.

Finish here.
