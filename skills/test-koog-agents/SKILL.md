---
name: test-koog-agents
description: >
  Test Koog 1.2 agents deterministically — install `agents-test`, mock the prompt
  executor with scripted responses, inject a fake `KoogClock` for time-sensitive
  logic, and assert on tool-call sequences. Use when the user asks to "test the
  agent", "mock the LLM in tests", "write unit tests for my Koog agent", or
  describes flaky or expensive tests that hit a real LLM.
---

# Test Koog Agents Skill

Process steps in order. Do not skip ahead.

## Step 1 — Add the Test Dependency

```kotlin
testImplementation("ai.koog:agents-test:1.2.0")
```

This is a `testImplementation` — it should never appear in production classpath.

Proceed immediately to Step 2.

## Step 2 — Mock the Prompt Executor

Real LLM calls in tests are slow, expensive, and non-deterministic. Replace `simpleOpenAIExecutor(...)` (or any other provider executor) with a scripted mock:

```kotlin
import ai.koog.agents.test.TestPromptExecutor
import ai.koog.prompt.model.Message

val executor = TestPromptExecutor.scripted(
    // First LLM call: agent picks a tool
    response = Message.Assistant.toolCalls(
        toolCalls = listOf(ToolCall(name = "lookup_account", args = mapOf("id" to "acc-42")))
    ),
    // Second LLM call (after tool result returns): agent replies with text
    response = Message.Assistant.text("Account acc-42 is on the Pro tier."),
)

val agent = AIAgent(
    promptExecutor = executor,
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = ToolRegistry { tools(MyTools().asTools()) },
    systemPrompt = "...",
)
```

Each scripted response is consumed in order — the Nth LLM call gets the Nth response. The test fails if the agent makes more calls than scripted (good — surfaces unexpected loops).

Proceed immediately to Step 3.

## Step 3 — Inject a Fake Clock for Time-Sensitive Logic

If the agent reads the current time (logging timestamps, prompt augmenters that include "now"), inject a deterministic `KoogClock`:

```kotlin
import ai.koog.agents.core.clock.KoogClock
import kotlin.time.Instant

val fixedTime = Instant.parse("2026-05-25T12:00:00Z")
val fakeClock = KoogClock.fixed(fixedTime)

val agent = AIAgent(
    promptExecutor = executor,
    llmModel = ...,
    systemPrompt = "...",
    clock = fakeClock,
)
```

`KoogClock` replaced `kotlin.time.Clock` as the parameter type in 1.0 (#1925). Test doubles need to use `KoogClock` factories, not the generic `Clock` interface.

Proceed immediately to Step 4.

## Step 4 — Assert on Tool-Call Sequences

For verifying the agent made the right tool calls, attach a recording event handler in the test:

```kotlin
import ai.koog.agents.features.eventhandler.handleEvents

val toolCalls = mutableListOf<String>()

val agent = AIAgent(
    promptExecutor = executor,
    llmModel = ...,
    toolRegistry = ...,
    systemPrompt = "...",
) {
    handleEvents {
        onToolCallStarting { ctx -> toolCalls.add(ctx.toolName) }
    }
}

agent.run("Look up account acc-42")

assertEquals(listOf("lookup_account"), toolCalls)
```

This pattern keeps assertions on observable behavior (which tools, in which order) rather than internal state — which makes the tests robust to refactors of the strategy.

Proceed immediately to Step 5.

## Step 5 — Avoid Mocking Tools Beyond What the Agent Sees

Tools should usually run their real implementations in tests — that's part of what's under test. Mock the LLM (Step 2), not the tools. If a tool itself depends on something expensive (a real database, network call), mock at that boundary inside the tool — keep the tool's interface to the agent unchanged.

The exception: tools that have external side effects (send email, post to Slack). For those, mock the underlying client and assert the tool was called with the right args via the event handler in Step 4.

Reference: `agents-test` ships further utilities — recorder executors, deterministic schedulers — that are useful for richer scenarios. The four primitives above (scripted executor, fake clock, event-handler recorder, real-tool defaults) cover most agent tests.

Finish here.
