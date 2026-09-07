---
name: add-token-budgeting
description: >
  Add token-budgeting and per-provider tokenizer support to a Koog 1.2 agent —
  install the tokenizer feature, set per-run or per-node budgets, and react to budget
  exhaustion (compress history, abort, swap models). Use when the user asks to "limit
  tokens per run", "add a token budget", "prevent runaway agent costs", "use a
  tokenizer", or describes cost containment requirements.
---

# Add Token Budgeting Skill

Process steps in order. Do not skip ahead.

## Step 1 — Add the Dependencies

```kotlin
implementation("ai.koog:agents-features-tokenizer:1.2.0")
implementation("ai.koog:prompt-tokenizer:1.2.0")    // provider tokenizers
```

The `prompt-tokenizer` module ships tokenizers for the major providers — they compute token counts before the LLM call, which is what the budgeting feature uses to gate requests.

Proceed immediately to Step 2.

## Step 2 — Install the Tokenizer Feature

```kotlin
import ai.koog.agents.features.tokenizer.Tokenizer

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = OpenAIModels.Chat.GPT4o,
    systemPrompt = "...",
) {
    install(Tokenizer) {
        // The tokenizer is selected per model by default; override if your provider needs custom counting
        runBudget = 50_000        // hard cap on tokens consumed across the whole agent run
        perNodeBudget = 8_000     // optional finer-grained cap per node
        onBudgetExceeded = BudgetAction.Abort   // or .CompressHistory, .DowngradeModel
    }
}
```

Budgets are inclusive — they count prompt tokens AND completion tokens. A 50k run budget against a 10k system prompt leaves 40k for the rest of the run.

Proceed immediately to Step 3.

## Step 3 — Choose the Budget-Exceeded Action

- `BudgetAction.Abort` — throws an exception, agent run ends with an error. Use when overrunning the budget is a bug, not an expected condition
- `BudgetAction.CompressHistory` — runs a history compression strategy (see `manage-state`) to reclaim budget, then continues. Use for long-running agents where the budget is soft
- `BudgetAction.DowngradeModel` — swaps to a cheaper model for subsequent calls. Use when output quality can degrade gracefully

For finer-grained behavior, hook the tokenizer's events through `handle-agent-events` — the tokenizer emits `onBudgetWarning` events before exhaustion, so you can take custom action (notify, switch tools, log).

Proceed immediately to Step 4.

## Step 4 — Surface Budget Through Observability

If OpenTelemetry is installed (`add-observability`), the tokenizer's token counts surface alongside the built-in `gen_ai.client.token.usage` metric — dashboards already targeted at that metric pick up budget data without changes.

If you only have the event handler installed (`handle-agent-events`), pair it with an `onBudgetWarning` callback to log warnings to stdout during development.

Finish here.
