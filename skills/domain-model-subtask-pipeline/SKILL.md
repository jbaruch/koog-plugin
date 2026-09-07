---
name: domain-model-subtask-pipeline
description: >
  Author a Koog 1.2 agent as a typed pipeline of domain-modeled subtasks — tools
  sliced by access pattern (read / write / communication) into separate ToolSets,
  inter-subtask handoffs as `@Serializable` `@LLMDescription`-annotated data
  classes (not text prompts), each subtask wired with `subgraphWithTask[In, Out]`
  using its own model and tool subset, self-correction loops via
  `subgraphWithVerification[T]` + `CriticResult[T]`. The integration pattern
  Koog's own banking demo uses. Use when the user asks to "model the agent as a
  pipeline", "build a multi-stage agent with typed handoffs", "give each stage
  its own tools", "build a verify-and-fix loop with typed data", or describes a
  workflow with distinct phases that should hand structured data to each other.
---

# Domain Model Subtask Pipeline Skill

Process steps in order. Do not skip ahead.

Worked Kotlin example excerpts (illustrative, with `...` placeholders for tool
bodies and prompt-executor wiring; not a standalone buildable file):

```text
skills/domain-model-subtask-pipeline/references/banking-example.md
```

## Step 1 — Confirm the Shape Fits

The pattern earns its complexity when the work has **distinct phases** that
produce structurally different intermediate artifacts. Examples: issue triage
(raw text → classified issue → fix proposal → verified fix); customer support
(question → identified problem → applied resolution → verified resolution);
code review (PR diff → categorized changes → review comments → applied edits).

- Don't reach for this when the work is one-shot text-in-text-out. Use the
  default `singleRunStrategy()`
- Don't reach for it when the topology depends on runtime findings. Invoke
  `Skill(skill: "use-planner")`

Proceed immediately to Step 2.

## Step 2 — Slice Tools by Access Pattern

The slicing axis is **access**, not feature. Group tools into separate `ToolSet`
classes by what they can do:

- **Communication tools** — ask the user, send email, post comments, request approval
- **Read tools** — query, list, search, get details (no side effects)
- **Write tools** — mutate, create, delete, transfer (real consequences)

- Constructor parameters are dependency injection. They are NOT visible to the
  LLM
- `userId`, `sessionId`, DB connections, HTTP clients go through the constructor
- The LLM only sees `@Tool`-annotated methods
- Keeps authorization context out of the LLM-controlled surface

For tool registration mechanics, invoke `Skill(skill: "add-tool")`. See the
reference file for a worked three-`ToolSet` example.

Proceed immediately to Step 3.

## Step 3 — Define Typed Handoff Contracts

Each subtask's input and output is a `@Serializable` Kotlin data class with
`@LLMDescription` on the class and on every property.

- Class-level `@LLMDescription` describes the contract
- Property-level `@LLMDescription` describes each field
- The LLM produces typed JSON matching the schema
- Downstream subtasks consume the typed value
- The compiler verifies the chain. A type mismatch between subtasks doesn't
  compile

For top-level structured output (no subtask pipeline), invoke
`Skill(skill: "add-structured-output")`. See the reference file for worked
handoff classes.

Proceed immediately to Step 4.

## Step 4 — Build Each Subtask with `subgraphWithTask<In, Out>`

Each subtask is a `subgraphWithTask` (or `subgraphWithVerification`, see
Step 5) typed end-to-end. Pass the subset of tools the subtask needs plus the
model that fits the subtask's complexity:

```kotlin
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.ext.agent.subgraphWithVerification
import ai.koog.agents.ext.agent.CriticResult

val identifyProblem by subgraphWithTask<String, AccountIssueSummary>(
    tools = communicationTools.asTools() + readTools.asTools(),
    llmModel = OpenAIModels.Chat.GPT5_2,
) { input -> "Identify the problem:\n$input" }
```

Match the model to the subtask:

- Cheap classification / extraction → fast small models (GPT-5 mini, Haiku, Flash)
- Action / generation → mid-tier (Sonnet, GPT-5)
- Verification / reasoning → reasoning tier (O3, Opus, GPT-5 Pro)

`subgraphWithTask`, `subgraphWithVerification`, and `CriticResult` live in
package `ai.koog.agents.ext.agent` but ship inside the **`agents-core`**
artifact, which the `koog-agents` umbrella already pulls. No extra
dependency needed — the standalone `ai.koog:agents-ext` artifact is a
separate beta-line module and is NOT required for these APIs.

If you type-annotate the returned strategy, the strategy type lives in
`ai.koog.agents.core.agent.entity.AIAgentGraphStrategy` (not bare
`ai.koog.agents.core.agent`).

For graph DSL mechanics (edges, node naming, the member-vs-extension
import table), invoke `Skill(skill: "author-strategy")`.

Proceed immediately to Step 5.

## Step 5 — Add a Verify-and-Adjust Loop with `subgraphWithVerification`

`subgraphWithVerification<T>` produces a `CriticResult<T>` with
`.successful: Boolean`, `.feedback: String?`, `.input: T`. Branch on it:

```kotlin
edge(verifySolution forwardTo nodeFinish onCondition { it.successful } transformed { it.input })
edge(verifySolution forwardTo adjustSolution onCondition { !it.successful } transformed { it.feedback.orEmpty() })
edge(adjustSolution forwardTo verifySolution)
```

- `transformed { it.input }` pulls the verified payload out of `CriticResult<T>`
- `transformed { it.feedback.orEmpty() }` coerces nullable feedback to non-null
  for `subgraphWithTask<String, _>` input
- The adjust→verify back-edge closes the loop

**The adjust subgraph re-runs the action. Grant it read + write `ToolSet`s.**
Communication-only adjustment cannot apply the corrected fix.

**Cap the loop.** A stubborn critic / adjuster can ping-pong forever within
`maxIterations`.

- Track attempts in `AIAgentStorage` (see `Skill(skill: "manage-state")`)
- Add an edge from `adjustSolution` to `nodeFinish` when attempts exceed a
  sensible bound

Proceed immediately to Step 6.

## Step 6 — Trust the Auto-Shared Message History

Koog shares the message history across subtasks automatically, even when each
subtask uses a different model. You do NOT thread the history manually between
subgraphs. Tool calls from `identifyProblem` are visible to `fixProblem`'s
LLM; `fixProblem`'s actions are visible to `verifySolution`.

Subgraphs are different from independent agents:

- A Koog subgraph (`subgraphWithTask` / `subgraphWithVerification`) is part of one agent. All subgraphs read the same shared message history
- A Koog sub-agent (`AIAgentService.fromAgent`, covered by `Skill(skill: "add-tool")` Step 3) is a separate agent. The parent and child communicate only through the typed input/output of the tool call
- LangChain4j Agentic sub-agents follow the same independent-agent model — input/output handoffs, no shared history
- The typed handoffs in Step 3 carry the structured artifact each phase produces. The shared history carries the surrounding context for free

If you actually want isolation (a phase that should NOT see prior context),
use a sub-agent (`Skill(skill: "add-tool")` Step 3), not a subgraph.

If the chain is long, history grows. Compress at deliberate boundaries (end of
a phase, start of the next), not at every node:

```kotlin
llm.writeSession {
    replaceHistoryWithTLDR()
}
```

Pass a `HistoryCompressionStrategy` variant when the default TL;DR shape
doesn't fit. For the full set of variants, invoke
`Skill(skill: "manage-state")`.

Proceed immediately to Step 7.

## Step 7 — Wire the Strategy into `AIAgent`

Pass the strategy explicitly as `strategy =`. It is not the default.

- Register every tool at the agent level via `toolRegistry`
- Each subgraph selects its subset via `subgraphWithTask`'s `tools =` parameter
- The agent's registry is one flat namespace. Don't register tools per-subgraph
- Set `maxIterations` higher than the default 50. Pipelines chain LLM calls
  and the default runs out fast
- When per-subgraph `llmModel` values span providers (e.g., `OpenAIModels.Chat.O3` for the verifier, `AnthropicModels.Sonnet_4_5` for the deployer), the agent needs a `MultiLLMPromptExecutor` with one client per provider. `simpleOpenAIExecutor` / `simpleAnthropicExecutor` only know one provider each and will fail at runtime when a subgraph asks for the other:

  ```kotlin
  import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
  import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
  import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor

  val executor = MultiLLMPromptExecutor(
      OpenAILLMClient(System.getenv("OPENAI_API_KEY")),
      AnthropicLLMClient(System.getenv("ANTHROPIC_API_KEY")),
  )
  ```

Run `./gradlew build`. The most common failure is a type mismatch on an edge
predicate. The chain only compiles when every subgraph's output type matches
the next subgraph's input type. Go back to Step 3 if types don't line up.

Full agent-construction snippet:

```text
skills/domain-model-subtask-pipeline/references/banking-example.md
```

Finish here.
