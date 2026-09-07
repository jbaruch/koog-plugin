---
name: add-observability
description: >
  Install OpenTelemetry observability into a Koog 1.2 agent — the multiplatform
  feature, the GenAI span/metric vocabulary, and one of the built-in backend
  integrations (Langfuse, Weave, Datadog, raw OTLP). Use when the user asks to
  "add telemetry", "wire up observability", "send traces to Langfuse", "add OpenTelemetry",
  "instrument the agent", or names any specific backend.
---

# Add Observability Skill

Process steps in order. Do not skip ahead.

## Step 1 — Add the Dependency

```kotlin
implementation("ai.koog:agents-features-opentelemetry:1.2.0")
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

```kotlin
import ai.koog.agents.features.opentelemetry.OpenTelemetry
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporter

val agent = AIAgent(
    promptExecutor = ...,
    llmModel = ...,
    systemPrompt = "...",
) {
    install(OpenTelemetry) {
        setVerbose(true)  // emit prompts, completions, and token counts on each span
        addLangfuseExporter(
            traceAttributes = listOf(
                CustomAttribute("langfuse.session.id", System.getenv("LANGFUSE_SESSION_ID") ?: ""),
            )
        )
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

JVM-only configuration lives on `OpenTelemetryConfigJvm` extensions (`addSpanExporter`, `addMetricExporter`, `addMetricFilter`). These are NOT visible in common code; if your project is JVM-only the extensions resolve normally:

```kotlin
import ai.koog.agents.features.opentelemetry.OpenTelemetryConfigJvm.addSpanExporter

install(OpenTelemetry) {
    addLangfuseExporter(...)
    // additional JVM-only span exporter
    addSpanExporter(MyCustomExporter())
    setShutdownOnAgentClose(true)  // opt-in JVM shutdown flush — the auto-hook was removed in 1.0
}
```

The JVM shutdown hook is no longer installed automatically — opt in with `setShutdownOnAgentClose(true)` if you want traces flushed when the JVM exits.

Proceed immediately to Step 5.

## Step 5 — Know the Built-In Vocabulary

Koog emits these GenAI metrics out of the box (target your dashboards at these names):

- `gen_ai.client.token.usage`
- `gen_ai.client.operation.duration`
- `gen_ai.client.tool.count`

Span attributes follow current OTel GenAI conventions: `gen_ai.input.messages` / `gen_ai.output.messages` (per-message span events were deprecated in 1.0).

If the user wants per-step structured logging on top (e.g., to stdout during development), pair this skill with `handle-agent-events`. OpenTelemetry is for production signal; events are for development surface.

Finish here.
