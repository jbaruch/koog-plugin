---
name: define-prompt
description: >
  Author prompts for a Koog 1.2 agent using the `prompt { ... }` DSL — system messages,
  user turns, few-shot examples, mixed media, and runtime augmentation via the
  `SystemPromptAugmenter` / `UserPromptAugmenter` family. Use when the user asks to
  "write a system prompt with examples", "add few-shot examples", "build a prompt",
  "augment the prompt at runtime", or moves beyond the single-string `systemPrompt`
  parameter on the factory.
---

# Define Prompt Skill

This skill is an action router — pick the step that matches the user's intent and execute only that step. Do not run other steps; do not parallelize.

Available actions:

- **Step 1** — Single-string `systemPrompt` (default — when a one-paragraph instruction is enough)
- **Step 2** — `prompt { ... }` builder with few-shot examples and structured turns
- **Step 3** — Runtime augmentation via `PromptAugmenter`

## Step 1 — Single-String `systemPrompt`

If the user just wants to set an instruction string, the factory's `systemPrompt` parameter is the right surface. No DSL needed:

```kotlin
val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = """
        You are a GitHub triage assistant. Classify issues, suggest labels,
        and link related issues by number when relevant.
    """.trimIndent(),
)
```

If the user is reaching for more structure than a single string supports, escalate to Step 2.

Finish here.

## Step 2 — `prompt { ... }` Builder

Use the DSL when you need system + assistant + user turns interleaved (few-shot examples), or when the same prompt shape is reused across multiple agents.

```kotlin
import ai.koog.prompt.dsl.prompt

val triagePrompt = prompt("issue-triage") {
    system("You are a GitHub triage assistant. Classify issues as bug, feature, or question.")

    // Few-shot example 1
    user("App crashes on Windows when I open the Settings dialog.")
    assistant("""{"classification": "bug", "confidence": 0.95}""")

    // Few-shot example 2
    user("Could we add dark mode to the export view?")
    assistant("""{"classification": "feature", "confidence": 0.9}""")

    // The actual turn is appended at runtime by the agent
}
```

The `Prompt` class lives in `ai.koog.prompt.Prompt` as of 1.0 (moved from `ai.koog.prompt.dsl.Prompt`, #2022); the `prompt { ... }` builder DSL stays in `ai.koog.prompt.dsl`.

Pass the built `Prompt` into the agent's strategy or context — the exact wiring point depends on whether the prompt is the agent's system prompt or a per-node call. For per-node use inside a strategy, send the prompt through `llm.writeSession { appendPrompt { ... } }`.

Finish here.

## Step 3 — Runtime Augmentation via `PromptAugmenter`

When the prompt needs runtime data (user metadata, current time, retrieved facts), use the augmenter family rather than string-templating into a `systemPrompt`. The augmenter appends a `MessagePart` to an existing `Message` — message-level rewrites from earlier versions are gone.

Variants:

- `SystemPromptAugmenter` — append to the system message
- `UserPromptAugmenter` — append to the next user turn
- `AgentcorePromptAugmenter` — append for Bedrock AgentCore flows

```kotlin
import ai.koog.prompt.processor.SystemPromptAugmenter

class TimestampAugmenter : SystemPromptAugmenter {
    override fun augment(): MessagePart {
        return MessagePart.text("Current time is ${java.time.Instant.now()}.")
    }
}

// Install via the agent's prompt processor surface (see prompt-processor module)
```

Augmenters run on every agent invocation — keep them cheap. Heavy fact retrieval belongs in `LongTermMemory`'s search pipeline (see `manage-state`), not in an augmenter.

Finish here.
