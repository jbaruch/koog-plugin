---
name: use-functional-agent
description: >
  Build a Koog 1.3 FunctionalAIAgent with the AIAgent factory and functionalStrategy.
  Put a suspending LLM request and result transformation in ordinary Kotlin control
  flow. Use when the user asks to "skip the graph DSL", "write the agent body as
  plain code", "use functionalStrategy", "use AIAgentFunctionalStrategy", or build
  a one-shot agent. For explicit nodes and edges, use GraphAIAgent instead.
---

# Use Functional Agent Skill

Process steps in order. Do not skip ahead.

## Step 1 — Select the Agent Shape

- Use `FunctionalAIAgent` for a suspending Kotlin body with direct LLM requests.
- Use `GraphAIAgent` for explicit nodes, edges and subgraphs.
- Use `PlannerAIAgent` for runtime planning.

Proceed immediately to Step 2.

## Step 2 — Create the Functional Strategy

Pass `functionalStrategy` to the top-level `AIAgent` factory. Supply the caller's
executor; retain responsibility for closing that executor after its agents finish.

<!-- compile-example: FunctionalAgent -->
```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.FunctionalAIAgent
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.MessagePart

fun formalEnglishAgent(executor: PromptExecutor): FunctionalAIAgent<String, String> =
    AIAgent<String, String>(
        promptExecutor = executor,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "Rewrite the input in formal English. Return only the rewrite.",
        strategy = functionalStrategy {
            val response = requestLLM(it)
            response.parts.filterIsInstance<MessagePart.Text>()
                .joinToString("\n") { part -> part.text }.trim()
        },
    )

suspend fun rewrite(executor: PromptExecutor, input: String): String {
    val agent = formalEnglishAgent(executor)
    return try {
        agent.run(input)
    } finally {
        agent.close()
    }
}
```

The strategy receiver is `AIAgentFunctionalContext`. `requestLLM` adds the input to
the prompt and requests a response. Read text from `MessagePart.Text` parts.

Proceed immediately to Step 3.

## Step 3 — Check Feature Compatibility

- Install features through the factory's `FunctionalAIAgent.FeatureContext`.
- Confirm each selected feature accepts that context; compile its installation.
- Expect no graph node or edge events from this agent shape.
- For tools, register a `ToolRegistry` and implement a bounded loop using
  `getToolCalls`, `executeTools` and `sendToolResults`.
- Use a graph strategy when node-specific checkpoints or graph transitions are required.

Proceed immediately to Step 4.

## Step 4 — Verify the Agent

Run `./gradlew build`. Exercise `rewrite` with a mock executor returning a known
text response; verify the trimmed rewrite and agent cleanup. Exercise any tool
loop's termination bound and registered tools separately. If compilation or a
behavior check fails, correct the factory, imports or strategy and repeat the checks.

Finish here.
