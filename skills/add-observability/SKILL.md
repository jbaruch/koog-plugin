---
name: add-observability
description: >
  Install OpenTelemetry observability into a Koog 1.3 agent — the multiplatform
  feature, the GenAI span/metric vocabulary, and one of the built-in backend
  integrations (Langfuse, Weave, Datadog, raw OTLP). Use when the user asks to
  "add telemetry", "wire up observability", "send traces to Langfuse", "add OpenTelemetry",
  "instrument the agent", or names any specific backend.
---

# Add Observability Skill

Process steps in order. Do not skip ahead.

## Step 1 — Add the Dependency

```kotlin
implementation("ai.koog:agents-features-opentelemetry:1.3.0")
```

The umbrella `koog-agents` does not include observability — add it explicitly.

Proceed immediately to Step 2.

## Step 2 — Pick a Backend

Use whichever backend the user named. If the user did not name one, default to OTLP. Do not block on a clarifying question.

- Langfuse — hosted LLM-observability product; needs project URL + keys via env vars
- Weave — Weights & Biases LLM observability
- Datadog — for orgs that already use Datadog APM
- OTLP — raw OpenTelemetry Protocol endpoint; works with any compliant collector

Proceed immediately to Step 3. Step 3 writes the actual code to disk; do not stop at prose.

## Step 3 — Install the Feature

Write the modified agent construction and the dependency to disk — do not respond with prose only. Use explicit `Path:` labels (same convention as `scaffold-agent`):

- `Path: src/main/kotlin/com/example/Main.kt` — modified agent construction (or whichever file contains the `AIAgent(...)` call)
- `Path: build.gradle.kts` — appended dependency line

Create files if they don't exist.

Install inside the `AIAgent(...)` trailing lambda. The feature is multiplatform (#1942 in 1.0), so the common-code block stays portable; JVM-only knobs come in Step 4.

**Langfuse:**

<!-- compile-example: Telemetry -->
```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.opentelemetry.attribute.CustomAttribute
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporter
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor

fun telemetryAgent(executor: PromptExecutor) = AIAgent(
    promptExecutor = executor,
    llmModel = OpenAIModels.Chat.GPT4o,
    systemPrompt = "You are a helpful assistant.",
) {
    install(OpenTelemetry) {
        setVerbose(true)  // include available prompt, completion, and response metadata
        addLangfuseExporter(
            traceAttributes = listOf(
                CustomAttribute("langfuse.session.id", System.getenv("LANGFUSE_SESSION_ID") ?: ""),
            )
        )
        setShutdownOnAgentClose(true)
    }
}
```

**OTLP (raw):**

```kotlin
import ai.koog.agents.features.opentelemetry.integration.otlp.addOtlpExporter

install(OpenTelemetry) {
    addOtlpExporter(endpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT"))
}
```

**Weave** / **Datadog**: the corresponding `add<Backend>Exporter` functions live in `feature/opentelemetry/integration/<backend>/`. Use the env-var pattern for keys.

Don't stack two backends — pick one. Stacking compounds traces and inflates cost without adding signal.

Proceed immediately to Step 4.

## Step 4 — JVM-Only Tuning (Optional)

JVM-specific exporter overloads are members of
`ai.koog.agents.features.opentelemetry.feature.OpenTelemetryConfig`.
There is no `OpenTelemetryConfigJvm` class and no extension import for its members.

<!-- compile-example: TelemetryConfig -->
```kotlin
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetryConfig
import io.opentelemetry.sdk.trace.export.SpanExporter

fun configureExporter(config: OpenTelemetryConfig, exporter: SpanExporter) {
    config.addSpanExporter(exporter)
    config.setShutdownOnAgentClose(true)
}
```

`setShutdownOnAgentClose(true)` shuts down telemetry when the owning agent closes.
It does not register a JVM exit hook. Close the agent explicitly; coordinate
telemetry ownership when several agents share a configuration.

Koog 1.3 carries Google's `cachedContentTokenCount` into response metadata and
OpenTelemetry, and fixes signature-only Gemini reasoning parts in Langfuse traces.
CLI processes may not report complete token/cost metadata. Instrument application
actions separately when the required signal is outside Koog's agent events.

Proceed immediately to Step 5.

## Step 5 — Know the Built-In Vocabulary

Koog emits these GenAI metrics out of the box (target your dashboards at these names):

- `gen_ai.client.token.usage`
- `gen_ai.client.operation.duration`
- `gen_ai.client.tool.count`

Span attributes follow current OTel GenAI conventions: `gen_ai.input.messages` / `gen_ai.output.messages` (per-message span events were deprecated in 1.0).

If the user wants per-step structured logging on top (e.g., to stdout during development), pair this skill with `handle-agent-events`. OpenTelemetry is for production signal; events are for development surface.

Finish here.
