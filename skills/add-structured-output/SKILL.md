---
name: add-structured-output
description: >
  Get typed structured output from a Koog 1.2 agent — pick between
  `nodeLLMRequestStructured` (graph DSL, schema-driven JSON) and `responseProcessor`
  (top-level on the agent factory, simpler shape). Defines the `@Serializable`
  output class and wires it into the strategy or the factory. Use when the user
  asks to "return JSON", "get a typed response", "use structured output",
  "return a data class from the agent", or shows a `data class` they want as
  the agent's output.
---

# Add Structured Output Skill

This skill is an action router — pick the step that matches the user's intent and execute only that step. Do not run other steps; do not parallelize.

Available actions:

- **Step 1** — Top-level `responseProcessor` (simpler — works with default `singleRunStrategy`)
- **Step 2** — `nodeLLMRequestStructured` (graph DSL — required when the structured call sits inside a custom strategy)

## Step 1 — Top-Level `responseProcessor`

Use when the user just wants the agent's final answer to be a typed object and they're not authoring a custom strategy.

Define the output type with `kotlinx.serialization`:

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class TriageResult(
    val classification: String,
    val confidence: Double,
    val suggestedAction: String,
)
```

Pass a `ResponseProcessor` configured for the type to the factory:

```kotlin
val agent: AIAgent<String, TriageResult> = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = "Classify the GitHub issue and suggest an action.",
    responseProcessor = jsonResponseProcessor<TriageResult>(),
)

val result: TriageResult = agent.run("Issue: app crashes on startup ...")
```

The output type parameter on `AIAgent<Input, Output>` becomes the result of `agent.run(...)`. Use this path when the strategy is `singleRunStrategy()` (the default).

Write the result to disk with explicit `Path:` labels (same convention as `scaffold-agent`). Do not respond with prose only.

- `Path: src/main/kotlin/com/example/Main.kt` — the full agent construction with the `responseProcessor` parameter and the `@Serializable` output type
- `Path: build.gradle.kts` — the `kotlinx-serialization` plugin and dependency when not already present

Create the files if they do not exist.

Finish here.

## Step 2 — `nodeLLMRequestStructured` Inside a Strategy

Use when the structured call lives inside a custom graph — e.g., one phase of a multi-step workflow produces a typed object that feeds the next phase.

Define the output type:

```kotlin
@Serializable
data class Classification(
    val type: String,
    val tags: List<String>,
)
```

Use the structured node inside the strategy:

```kotlin
import ai.koog.agents.core.dsl.builder.strategy

val classifyStrategy = strategy<String, Classification>("classify-issue") {
    val classify by nodeLLMRequestStructured<Classification>()

    edge(nodeStart forwardTo classify)
    edge(classify forwardTo nodeFinish)
}
```

`nodeLLMRequestStructured<T>()` returns a `Classification`-typed value as the node output. The next edge's predicates/transforms operate on the typed value, not on raw `Message.User`.

Wire into the agent:

```kotlin
val agent: AIAgent<String, Classification> = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = "...",
    strategy = classifyStrategy,
)
```

The strategy's `Output` type parameter must match the structured node's type parameter and `AIAgent`'s `Output` parameter. Mismatches surface as compile errors.

Write the strategy and the modified agent construction to disk with explicit `Path:` labels (same convention as `scaffold-agent`). Do not respond with prose only.

- `Path: src/main/kotlin/com/example/Strategy.kt` — the strategy with the `nodeLLMRequestStructured` node and the `@Serializable` output type
- `Path: src/main/kotlin/com/example/Main.kt` — the agent construction passing `strategy = ...`
- `Path: build.gradle.kts` — the `kotlinx-serialization` plugin and dependency when not already present

Create the files if they do not exist.

Finish here.
