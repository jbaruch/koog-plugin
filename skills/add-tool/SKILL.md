---
name: add-tool
description: >
  Add a new tool to an existing Koog 1.2 agent. Pick the right registration style
  (@Tool + ToolSet annotation, Tool[TArgs,TResult] subclass, or sub-agent-as-tool),
  define the args, and wire the tool into the agent's ToolRegistry. Use when the user
  asks to "add a tool to my agent", "expose something to the LLM", "let the agent call
  a function", or "wrap this agent as a tool for another agent". Assumes a scaffolded
  Koog 1.2 project — for new projects start with the scaffold-agent skill.
---

# Add Tool Skill

This skill is an action router — pick the step that matches the user's intent and execute only that step. Do not run other steps; do not parallelize.

Available actions:

- **Step 1** — Annotated tool (`@Tool` on a `ToolSet` class): the right default for tools that take JSON-shaped args and return a stringifiable result
- **Step 2** — Typed tool (subclass `Tool<TArgs, TResult>`): when you need typed arguments with custom serialization or programmatic schema control
- **Step 3** — Sub-agent-as-tool (wrap another `AIAgent` as a tool): when the step is itself agent-shaped — its own tools, its own model, its own strategy

If the user is ambiguous, default to Step 1 (annotated). Escalate to Step 2 (typed `Tool<TArgs,TResult>`) when **any** of these is true:

- The user supplies an existing function whose parameter is a `data class`, not flat primitives
- The user's existing function returns a `data class` or other typed result
- The user names "typed args", "typed result", "Tool subclass", "ToolDescriptor", or "no String return"

Escalate to Step 3 only when the new "tool" is itself agent-shaped.

## Step 1 — Annotated Tool

Locate the agent's `ToolRegistry` (typically in the file that constructs `AIAgent(...)`). If the project has no `ToolSet` class yet, create one in the same package:

```kotlin
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTools

@LLMDescription("Tools for <one-line purpose of the group>")
class <Name>Tools : ToolSet {
    @Tool
    @LLMDescription("<one-line what this tool does, written for the LLM>")
    fun <toolName>(
        @LLMDescription("<arg purpose>") arg1: <Type>,
        @LLMDescription("<arg purpose>") arg2: <Type>,
    ): String {
        // implementation
        return "<result the LLM will see>"
    }
}
```

Then register in the agent construction:

```kotlin
val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    toolRegistry = ToolRegistry { tools(<Name>Tools().asTools()) },
    systemPrompt = ...,
)
```

If the Kotlin function name doesn't read well to the LLM, add `@Tool(customName = "list_prs")`. As of Koog 1.2 this override is honored by `asTools()`.

Tools that return non-String types: convert at the boundary to a `String` (`toString()`, a JSON-encoded shape, etc.). The LLM only sees the returned string.

Finish here.

## Step 2 — Typed Tool (`Tool<TArgs, TResult>`)

Use when annotations don't fit — typed arguments with custom serialization, programmatic schema control, registration based on runtime data, or any case where the user's existing function takes a typed `data class` parameter.

Write the tool class and the modified agent construction to disk with explicit `Path:` labels (same convention as `scaffold-agent`):

- `Path: src/main/kotlin/com/example/AccountLookupTool.kt` — typed `Tool<TArgs,TResult>` subclass (rename to match the tool's actual name; do not write `<Name>Tool.kt` literally)
- `Path: src/main/kotlin/com/example/Main.kt` — modified agent construction registering the tool
- `Path: build.gradle.kts` — any new dependency lines

Create files if they don't exist. Do not respond with prose only. Replace placeholder names in the path with the user's actual tool name.

```kotlin
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.TypeToken
import kotlinx.serialization.Serializable

@Serializable
data class <Name>Args(val field1: String, val field2: Int)

@Serializable
data class <Name>Result(val output: String)

class <Name>Tool : Tool<<Name>Args, <Name>Result>(
    argsType = TypeToken.of(<Name>Args::class),
    resultType = TypeToken.of(<Name>Result::class),
    descriptor = ToolDescriptor(
        name = "<tool_name>",
        description = "<what the tool does>",
        // parameter descriptions if needed
    ),
) {
    override suspend fun execute(args: <Name>Args): <Name>Result {
        return <Name>Result(output = "...")
    }
}
```

Register:

```kotlin
toolRegistry = ToolRegistry { tool(<Name>Tool()) }
```

If you need raw `ToolCallMetadata` (per-call trace IDs, live `AIAgentContext`), subclass `ToolBase` directly rather than `Tool`. Reach for `ToolBase` only when `Tool` cannot express the dependency.

Finish here.

## Step 3 — Sub-Agent-as-Tool

Use when the new "tool" is itself agent-shaped: its own tools, model, strategy. Reference: `examples/code-agent/step-04-add-subagent/`.

**Sub-agent vs subgraph — pick the right primitive:**

- **Sub-agent** (this step, `AIAgentService.fromAgent`) — fully independent agent. Has its own context window; parent and child communicate only through the typed input/output of the tool call. Pick when the child should NOT see the parent's conversation history (isolation, separate scope, different system prompt that mustn't leak)
- **Subgraph** (`subgraphWithTask` / `subgraphWithVerification`, covered by `Skill(skill: "author-strategy")` and `Skill(skill: "domain-model-subtask-pipeline")`) — part of the SAME agent. All subgraphs share one message history, so each subgraph's LLM call sees what every prior subgraph saw and did. Pick when phases need shared context (typed-handoff pipelines, verify-and-fix loops, multi-stage classification)

The default for "I want to break my agent into stages" is **subgraph**, not sub-agent. Reach for sub-agent only when isolation is the explicit requirement.

Construct the child agent first (full `AIAgent(...)` factory call). Then wrap and register:

```kotlin
import ai.koog.agents.core.agent.AIAgentService

val childAgent: GraphAIAgent<String, String> = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    toolRegistry = ToolRegistry { tools(<ChildTools>().asTools()) },
    systemPrompt = "<child's focused system prompt>",
)

val childAsTool = AIAgentService
    .fromAgent(childAgent)
    .createAgentTool<String, String>(
        agentName = "__<child_name>_agent__",
        agentDescription = "<one-line what the child does, written for the parent's LLM>",
        inputDescription = "<one-line what input the parent should pass>",
    )

val parent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    toolRegistry = ToolRegistry { tool(childAsTool) },
    systemPrompt = ...,
)
```

Name convention: prefix child agent tool names with `__` (double underscore) per the in-repo example — this keeps them visually distinct from regular tools in logs.

Each child agent invocation is a full agent run with its own LLM round-trips. Budget `maxIterations` on the parent accordingly — agent-tools are not free.

Finish here.
