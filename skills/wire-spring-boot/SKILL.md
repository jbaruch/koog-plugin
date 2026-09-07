---
name: wire-spring-boot
description: >
  Wire Koog 1.2 into a Spring Boot application via `koog-spring-boot-starter` —
  per-provider autoconfig, `MultiLLMAutoConfiguration` aggregation, and the
  `application.yml` shape for agent name, model, system prompt, and tools (including
  MCP entries). Use when the user asks to "use Koog in Spring Boot", "wire the Spring
  starter", "configure providers via application.yml", "add Koog to my Spring app".
---

# Wire Spring Boot Skill

Process steps in order. Do not skip ahead.

## Step 1 — Add the Starter

```kotlin
implementation("ai.koog:koog-spring-boot-starter:1.2.0-beta")
```

The starter auto-configures one `LLMClient` bean per enabled provider and one `MultiLLMPromptExecutor` aggregating them. Adding it transitively pulls the per-provider auto-configurations.

Don't construct executors manually in a `@Configuration` class when the starter is on the classpath — you'll get two beans and Spring picks one unpredictably.

Proceed immediately to Step 2.

## Step 2 — Configure Providers in `application.yml`

Each provider keys off `ai.koog.<provider>.api-key` and `ai.koog.<provider>.enabled`. Both must be set for autoconfig to wire the client bean. Provider keys (match the autoconfig class names):

- `anthropic`, `deepseek`, `google`, `mistralai`, `ollama`, `openai`, `openrouter`

```yaml
ai:
  koog:
    google:
      api-key: ${GOOGLE_API_KEY}
      enabled: true
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      enabled: true
```

Always read keys from environment variables via `${VAR}` — never hardcode literals in YAML. The Anthropic autoconfig already masks the key in logs (security fix in 1.0); don't undo that mask with `INFO`-level logging in your own configuration code.

Proceed immediately to Step 3.

## Step 3 — Declare Agent Shape

The starter reads agent shape from `agent.*` keys in `application.yml`. Canonical layout (matches `examples/spring-boot-kotlin`):

```yaml
agent:
  version: "1.0.0"
  name: "github-assistant"
  model:
    id: "gemini-2.5-flash-lite"
    options:
      temperature: 1.0
  system_prompt: |
    You are a helpful GitHub assistant.
    Always cite the issue number when you reference an issue.
  tools:
    - type: MCP
      id: "GitHub"
      options:
        docker_image: "ghcr.io/github/github-mcp-server"
        docker_options:
          GITHUB_PERSONAL_ACCESS_TOKEN: ${GITHUB_PERSONAL_ACCESS_TOKEN}
```

For MCP tools that need secrets (tokens, API keys), reference an env var inside `docker_options` (or the equivalent for non-Docker MCP runners). Never inline the secret.

Proceed immediately to Step 4.

## Step 4 — Consume the Executor

Inject `MultiLLMPromptExecutor` (not individual `LLMClient` beans) into your service code — let it route by model:

```kotlin
@Service
class AgentService(private val executor: MultiLLMPromptExecutor) {
    suspend fun run(input: String): String {
        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = ...,
            systemPrompt = ...,
        )
        return agent.run(input)
    }
}
```

If you need to override a single provider's client (custom OkHttp config, retry policy), declare a `@Bean` of that provider's `LLMClient` type marked `@Primary` — the autoconfig backs off for that provider.

Proceed immediately to Step 5.

## Step 5 — Don't Confuse with Spring AI Starters

The `koog-spring-ai-*` family (`-common`, `-starter-model-chat`, `-starter-model-embedding`, `-starter-chat-memory`, `-starter-vector-store`) is a **bridge to Spring AI's interfaces**, not a replacement for `koog-spring-boot-starter`. Use it when an existing Spring AI app should swap in Koog as the backing executor while keeping Spring AI's APIs at the call site.

If the user mentioned "Spring AI" specifically, confirm which starter family they want before adding the wrong one. Finish here.
