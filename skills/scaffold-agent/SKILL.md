---
name: scaffold-agent
description: >
  Bootstrap a new Koog 1.2 Kotlin agent project from scratch: Gradle setup with the right
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
    implementation("ai.koog:koog-agents:1.2.0")
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

If the user picked a provider that requires a non-umbrella client artifact, add that dependency explicitly (e.g., `implementation("ai.koog:prompt-executor-litert-client:1.2.0-beta")`).

Write `settings.gradle.kts` with `rootProject.name = "<directory-name>"` and a single `include(":")` is not needed for a flat project — leave it as just `rootProject.name = ...`.

Proceed immediately to Step 4.

## Step 4 — Write `Main.kt`

Path: `src/main/kotlin/com/example/Main.kt`. Use the canonical 1.0 form — top-level `AIAgent(...)` factory, env-var API key, `singleRunStrategy()` is the default so no explicit `strategy=` parameter:

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

For Anthropic, swap `simpleOpenAIExecutor` → `simpleAnthropicExecutor`, `OpenAIModels.Chat.GPT4o` → `AnthropicModels.Opus_4_7`, and the env var name. For Google: `simpleGoogleAIExecutor` + `GoogleModels.Chat.Gemini_2_5_FlashLite`. For Ollama: `simpleOllamaAIExecutor(baseUrl = "http://localhost:11434")` and an `OllamaModels.*` entry; no API key needed.

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

A Koog 1.2 agent. Set `<PROVIDER>_API_KEY` and run `./gradlew run`.
```

Proceed immediately to Step 6.

## Step 6 — Hand Off

- Run `./gradlew --version` to confirm the toolchain resolves. If Gradle isn't installed, point the user to `https://gradle.org/install/` and finish here
- If Gradle is installed, run `./gradlew build` once. If it fails, surface the exact error and the line in `build.gradle.kts` it refers to — do not auto-fix; coordinate dependency overrides come from the user
- If build passes, tell the user: "Set `<PROVIDER>_API_KEY` and run `./gradlew run` to invoke the agent." Finish here.

Do not chain into `add-tool` or `wire-mcp-server` — those are separate user invocations. If the user immediately asks for a tool or MCP after this, those skills handle it.
