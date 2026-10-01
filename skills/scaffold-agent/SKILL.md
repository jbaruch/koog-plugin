---
name: scaffold-agent
description: >
  Bootstrap a new Koog 1.3 Kotlin agent project from scratch: Gradle setup with the right
  dependencies, JDK 17 toolchain, application Main that constructs an AIAgent via the
  top-level factory, and an environment-variable wiring for the LLM API key. Use when the
  user asks to "create a new Koog agent", "start a Koog project", "scaffold an agent app",
  or provides a directory and says "set up Koog here". Produces a runnable hello-world
  agent that the user can extend with tools, strategies, or features. Do NOT use when
  the user is constructing a planner, picking a strategy variant, or naming a specific
  agent shape inside an existing project — use `use-planner` or `author-strategy` instead.
---

# Scaffold Agent Skill

Process steps in order. Do not skip ahead. This skill ends after Step 6 when the user has a runnable agent — do not chain into tool or MCP wiring; those are separate skills (`add-tool`, `wire-mcp-server`).

## Step 1 — Clarify the Target

Ask the user, one question at a time:

- Target directory — must be empty or not exist yet
- LLM provider for the starter agent — OpenAI, Anthropic, Google, Ollama (local), or other (the user supplies the provider's Koog factory name)
- Build tool — Gradle Kotlin DSL (recommended) or Gradle Groovy DSL

If the user picks "other", confirm the factory exists in `ai.koog:prompt-executor-llms-all` (the umbrella). If not, plan to pull the specific `prompt-executor-<provider>-client` module — note this for Step 3.

Proceed immediately to Step 2 once all three are answered.

## Step 2 — Verify the Target Directory

- If the path doesn't exist, create it
- If it exists, refuse to overwrite unless it is empty — silence is unsafe here; ask explicitly to confirm overwrite, and abort the skill on any answer that isn't "yes overwrite"
- Initialize git: `git init` in the target

Proceed immediately to Step 3.

## Step 3 — Write `build.gradle.kts`

Use this template, substituting `${provider}` placeholders:

```kotlin
plugins {
    kotlin("jvm") version "2.3.10"
    application
}

group = "com.example"
version = "0.1.0"

repositories { mavenCentral() }

dependencies {
    implementation("ai.koog:koog-agents:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.example.MainKt")
}
```

For Google, add both `ai.koog:prompt-executor-google-client:1.3.0-beta` and
`ai.koog:prompt-executor-llms-all:1.3.0-beta`. Add other non-umbrella client
artifacts explicitly when the selected provider requires them.

Write `settings.gradle.kts` with `rootProject.name = "<directory-name>"` and a single `include(":")` is not needed for a flat project — leave it as just `rootProject.name = ...`.

Proceed immediately to Step 4.

## Step 4 — Write `Main.kt`

Path: `src/main/kotlin/com/example/Main.kt`. Use the canonical 1.0 form — top-level `AIAgent(...)` factory, env-var API key, `singleRunStrategy()` is the default so no explicit `strategy=` parameter:

<!-- compile-example: Scaffold -->
```kotlin
package com.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val apiKey = System.getenv("OPENAI_API_KEY")
        ?: error("Set OPENAI_API_KEY in your environment")

    val agent = AIAgent(
        promptExecutor = simpleOpenAIExecutor(apiKey),
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a helpful assistant.",
    )

    val result = agent.run("Hello! What can you help me with?")
    println(result)
}
```

For Anthropic, use `simpleAnthropicExecutor`, `AnthropicModels.Opus_5`, and
`ANTHROPIC_API_KEY`. For Ollama, use `simpleOllamaAIExecutor(baseUrl =
"http://localhost:11434")` and an `OllamaModels.*` entry; no API key is required.

For Google, use the following construction with the dependencies from Step 3.
Koog 1.3 also provides `GoogleModels.Gemini3_6Flash` and `GoogleModels.Gemini3_7Flash`.

<!-- compile-example: GoogleScaffold -->
```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

fun googleAgent() = AIAgent(
    promptExecutor = simpleGoogleAIExecutor(
        requireNotNull(System.getenv("GOOGLE_API_KEY")) { "Set GOOGLE_API_KEY" },
    ),
    llmModel = GoogleModels.Gemini2_5FlashLite,
    systemPrompt = "You are a helpful assistant.",
)
```

Do NOT add `strategy = singleRunStrategy()` to the constructor — it's the default and listing it muddies the example. Add an explicit `strategy = ...` only when the user is overriding the default.

Proceed immediately to Step 5.

## Step 5 — Write Scaffold Static Files

`.gitignore`:

```
.gradle/
build/
.idea/
*.iml
.DS_Store
```

`README.md` (minimal — one paragraph, the run command, and the env-var requirement):

```markdown
# <project name>

A Koog 1.3 agent. Set `<PROVIDER>_API_KEY` and run `./gradlew run`.
```

Proceed immediately to Step 6.

## Step 6 — Hand Off

- Run `./gradlew --version` to confirm the toolchain resolves. If Gradle isn't installed, point the user to `https://gradle.org/install/` and finish here
- If Gradle is installed, run `./gradlew build` once. If it fails, surface the exact error and the line in `build.gradle.kts` it refers to — do not auto-fix; coordinate dependency overrides come from the user
- If build passes, tell the user: "Set `<PROVIDER>_API_KEY` and run `./gradlew run` to invoke the agent." Finish here.

Do not chain into `add-tool` or `wire-mcp-server` — those are separate user invocations. If the user immediately asks for a tool or MCP after this, those skills handle it.
