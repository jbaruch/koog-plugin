---
name: define-prompt
description: >
  Author prompts for a Koog 1.3 agent using the `prompt { ... }` DSL — system messages,
  user turns, few-shot examples, and runtime context through `llm.writeSession`.
  Use when the user asks to
  "write a system prompt with examples", "add few-shot examples", "build a prompt",
  "augment the prompt at runtime", or moves beyond the single-string `systemPrompt`
  parameter on the factory.
---

# Define Prompt Skill

This skill is an action router — pick the step that matches the user's intent and execute only that step. Do not run other steps; do not parallelize.

Available actions:

- **Step 1** — Single-string `systemPrompt` (default — when a one-paragraph instruction is enough)
- **Step 2** — `prompt { ... }` builder with few-shot examples and structured turns
- **Step 3** — Runtime context in a graph write session

## Step 1 — Single-String `systemPrompt`

If the user just wants to set an instruction string, the factory's `systemPrompt` parameter is the right surface. No DSL needed:

<!-- compile-example: SimplePrompt -->
```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor

fun triageAgent(executor: PromptExecutor) = AIAgent(
    promptExecutor = executor,
    llmModel = OpenAIModels.Chat.GPT4o,
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

<!-- compile-example: FewShotPrompt -->
```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor

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

fun fewShotAgent(executor: PromptExecutor) = AIAgent(
    promptExecutor = executor,
    agentConfig = AIAgentConfig(
        prompt = triagePrompt,
        model = OpenAIModels.Chat.GPT4o,
        maxAgentIterations = 50,
    ),
)
```

The `Prompt` class lives in `ai.koog.prompt.Prompt` as of 1.0 (moved from `ai.koog.prompt.dsl.Prompt`, #2022); the `prompt { ... }` builder DSL stays in `ai.koog.prompt.dsl`.

Pass a built `Prompt` through `AIAgentConfig.prompt`, as shown above.
For per-node context, use Step 3.

Finish here.

## Step 3 — Add Runtime Context in a Write Session

Add runtime data in the graph node where it becomes available. Pass the required
data explicitly; append context before the next LLM request.

<!-- compile-example: RuntimePrompt -->
```kotlin
import ai.koog.agents.core.agent.context.AIAgentGraphContextBase

// Call addAccountContext(this, accountContext) inside the boundary node.
suspend fun addAccountContext(context: AIAgentGraphContextBase, accountContext: String) {
    context.llm.writeSession {
        appendPrompt {
            system("Account context: $accountContext")
        }
    }
}
```

Call the helper once at the intended boundary. Use `user(...)` for untrusted
user-provided text instead of elevating it to a system instruction.
For cross-session retrieval, invoke `Skill(skill: "manage-state")` Step 3.
Its `SystemPromptAugmenter` / `UserPromptAugmenter` types belong to
`ai.koog.agents.longtermmemory.retrieval.augmentation` and are configured inside
`LongTermMemory`'s `retrieval { promptAugmenter = ... }` block.

Write the selected prompt and agent configuration to the user's source file.
Run `./gradlew build`; verify that the model receives the intended instruction,
few-shot turns or runtime context in the chosen agent/node.

Finish here.
