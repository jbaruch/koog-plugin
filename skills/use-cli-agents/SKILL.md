---
name: use-cli-agents
description: >
  Drive another vendor's CLI coding agent (Claude Code, OpenAI Codex, or an
  arbitrary binary) from inside a Koog agent using `CliAIAgent`, and compose it
  into a graph strategy with `.asNode()`. Authenticates through whatever
  subscription that CLI is already logged into, so no API key is needed. Use
  when the user asks to "call Claude Code from Koog", "use my Claude/Codex
  subscription instead of an API key", "orchestrate multiple coding agents",
  "use a different vendor for one step", or names `CliAIAgent` / `agents-cli`.
  New in Koog 1.1.1.
---

# Use CLI Agents Skill

Process steps in order. Do not skip ahead.

## Step 0 — Confirm this is the right tool

`CliAIAgent` shells out to a **coding-agent CLI** and parses its output. Reach for it
when you want a step to run on a vendor you have a *subscription* to rather than an
API key, or when you deliberately want a second vendor's judgement in the loop.

Do **not** reach for it as a general LLM call. It is one to two orders of magnitude
slower than an API call — budget 10-60s per invocation against 2-3s for a direct API
call.

If the request is an ordinary model call — one prompt, one completion, no second
vendor's judgement wanted — recommend a `PromptExecutor` and say why it is the better
fit. **Finish here.** Do not add `agents-cli`.

Continue only when the user wants a subscription-authenticated CLI or a deliberate
second-vendor step. Ask which CLI, and confirm it is installed and logged in before
writing code.

Proceed to Step 1.

## Step 1 — Add the Dependency

Path: `build.gradle.kts`

```kotlin
implementation("ai.koog:agents-cli:1.2.0-beta")
```

Beta version line, not `1.2.0`.

Proceed immediately to Step 2.

## Step 2 — Construct the Agent

Path: `Critic.kt` (or wherever the step lives)

```kotlin
import ai.koog.agents.cli.CliAIAgent
import ai.koog.agents.cli.transport.CliTransport
import ai.koog.agents.cli.claude.ClaudePermissionMode

val reviewer = CliAIAgent.claude(
    transport = CliTransport.default(),
    outputClass = Critique::class,        // @Serializable — gives typed output
    apiKey = null,                        // null = use the CLI's own auth. This is the point.
    permissionMode = ClaudePermissionMode.BypassPermissions,
    workspace = scratchDir,               // see Step 3
    systemPrompt = "...",
    generateRequest = { input -> "...$input" },   // MUST be named, see below
)
```

Four things that will each cost you a build:

- **`apiKey = null` is deliberate.** Passing a key sets `ANTHROPIC_API_KEY` /
  `CODEX_API_KEY` in the child environment and *overrides* the subscription. Leave it
  null to use the login
- **`generateRequest` must be a named argument.** As a trailing lambda it binds to
  `installFeatures` instead, and the error is a confusing arity mismatch on
  `FeatureContext`
- **`CliTransport.default()` is a function call**, not a property
- `codex` additionally needs `additionalFlags = listOf("--skip-git-repo-check")`
  unless the workspace is a trusted git repository

Constructors: `CliAIAgent.claude(...)`, `CliAIAgent.codex(...)`, and
`CliAIAgent.builder(transport)` for any other binary — the custom builder needs
`binaryPath`, `flags`, `generateRequest` and `extractOutput`.

Proceed immediately to Step 3.

## Step 3 — Pen It In With a Workspace

`workspace` defaults to `"."`. **These are coding agents: they will read the working
directory** — including files the step was never meant to depend on. That makes a
CLI-backed stage silently dependent on whatever happens to be nearby, and an
exfiltration path when the prompt is attacker-influenced.

```kotlin
private val scratchDir = Files.createTempDirectory("cli-agent")
    .toFile().apply { deleteOnExit() }.absolutePath
```

Give every CLI-backed step an empty scratch workspace unless it genuinely needs
project files.

Proceed immediately to Step 4.

## Step 4 — Compose Into a Graph

`.asNode()` turns the agent into a graph node. With `outputClass`, the node's output
type is `CliAgentStructuredResponse<T>`, and `structuredResult` is **nullable**.

Path: `Strategy.kt`

```kotlin
import ai.koog.agents.cli.asNode   // required import — asNode is an extension

val review by reviewer.asNode("review")

edge(draft forwardTo review)
edge(review forwardTo nodeFinish
        onCondition { it.structuredResult?.approved == true }
        transformed { lastDraft!! })
edge(review forwardTo fix
        onCondition { it.structuredResult?.approved != true }
        transformed { it.structuredResult?.feedback ?: "reviewer returned nothing parseable" })
```

**Fail closed.** A CLI agent that times out, crashes, or emits unparseable output
yields a null `structuredResult`. Treat null as *rejection*, never as approval —
`?.approved == true`, never `!= false`.

A structured response carries only what you declared. If the next node needs the thing
being reviewed, capture it on the inbound edge (`transformed { last = it; it }`); the
critique will not carry it for you.

The `review → fix → review` cycle above is unbounded as written. Bound it with a
run-scoped refusal counter and terminate on an explicit rejection — use
`Skill(skill: "author-strategy")` for that shape.

Proceed immediately to Step 5.

## Step 5 — Verify

1. The CLI answers non-interactively on its own first: `claude -p "say PONG"`,
   `codex exec --skip-git-repo-check "say PONG"`. Fix auth here, not in Kotlin
2. `structuredResult` is non-null on a normal run
3. Kill the CLI's auth and confirm the graph takes the rejection edge rather than
   approving
4. Time it. Budget the CLI step at 10-60s and set `timeout` accordingly

## Cost, latency and when not to

- Every invocation is a **fresh process and a cold context** — no prompt caching, no
  warm connection
- Subscription CLIs have their own rate limits, and a graph that retries will consume
  them fast
- A verify/refine loop with a CLI critic runs to several minutes end to end. Bound the
  loop and terminate on an explicit rejection — use `Skill(skill: "author-strategy")`
- If you need this in a server request path, you almost certainly want a
  `PromptExecutor` instead
