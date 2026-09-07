---
alwaysApply: true
---

# Module Coordinates

## Use 1.2, not 1.0, and never 0.x

- All Koog artifacts ship under group `ai.koog`. The current umbrella is
  **`ai.koog:koog-agents:1.2.0`** (released 2026-08-28)
- Never mix 0.x with 1.x — the API surface diverged at 1.0 (factory functions,
  planner module split, HTTP transport decoupling) and a mixed graph fails at link time
- 1.0 → 1.2 is **source-compatible** for the graph DSL: `subgraphWithTask`,
  `subgraphWithVerification`, `CriticResult`, `ToolSet`, MCP and memory all survive
  unchanged. The only removal is `PromptAugmenter.SECTION_SEPARATOR`, in a beta module
- The hosted Maven snippet on `docs.koog.ai/quickstart/` lags the release. Don't copy
  it — check `repo1.maven.org/maven2/ai/koog/koog-agents/maven-metadata.xml`

## Two version lines. This is the single biggest source of "could not find"

The umbrella is `1.2.0`. **A large and growing set of modules publishes only on the
`-beta` line, at `1.2.0-beta`.** `1.2.0` does not exist for them, and `1.2.0-beta`
does not exist for the umbrella. Getting this backwards is the most common build
failure on this framework.

| Module | Coordinate |
|---|---|
| umbrella | `ai.koog:koog-agents:1.2.0` |
| additions | `ai.koog:koog-agents-additions:1.2.0-beta` |
| MCP client | `ai.koog:agents-mcp:1.2.0-beta` |
| CLI agents | `ai.koog:agents-cli:1.2.0-beta` |
| Agent Skills | `ai.koog:skills:1.2.0-beta` |
| Google client | `ai.koog:prompt-executor-google-client:1.2.0-beta` |
| simple executors | `ai.koog:prompt-executor-llms-all:1.2.0-beta` |
| long-term memory | `ai.koog:agents-features-longterm-memory:1.2.0-beta` |
| file/dir tools | `ai.koog:agents-ext:1.2.0-beta` |
| planner | `ai.koog:agents-planner:1.2.0-beta` |
| Ktor plugin | `ai.koog:koog-ktor:1.2.0-beta` |
| Spring Boot starter | `ai.koog:koog-spring-boot-starter:1.2.0-beta` |
| MCP server | `ai.koog:agents-mcp-server:1.2.0-beta` |
| A2A | `ai.koog:a2a-core` / `-client` / `-server:1.2.0-beta` |
| vector RAG | `ai.koog:rag-vector:1.2.0-beta` |
| Redis prompt cache | `ai.koog:prompt-cache-redis:1.2.0-beta` |

Stable line (`1.2.0`), for contrast: `koog-agents`, `agents-test`, `rag-base`,
`embeddings-base`/`-llm`, `agents-features-opentelemetry`, `-snapshot`, `-trace`,
`-tokenizer`, `-sql`, `-event-handler`, `-persistence-jdbc`, `-chat-history-jdbc`,
`-chat-memory-sql`, `prompt-cache-files`, `prompt-executor-cached`, `prompt-tokenizer`,
`http-client-ktor`.

Note that the split does not follow "core vs satellite": `agents-features-chat-history-jdbc`
is stable while `agents-features-chat-history-aws` is beta, and the planner moved onto the
beta line after 1.0. Query the metadata; do not pattern-match on the name.

When a Koog dependency fails to resolve, **check the version line before anything
else**. Query the module's own `maven-metadata.xml`; do not assume it tracks the
umbrella.

## The umbrella does NOT bundle every provider

`koog-agents:1.2.0` pulls the clients for **OpenAI, Anthropic, Bedrock and Ollama**.
It does **not** pull Google.

- For Gemini you must add **both** `prompt-executor-google-client` and
  `prompt-executor-llms-all` (the latter is where `simpleGoogleAIExecutor` lives),
  both at `1.2.0-beta`
- Symptom when you forget: `Unresolved reference 'google'` and
  `Unresolved reference 'simpleGoogleAIExecutor'` while `AIAgent` itself resolves fine

## Do NOT add the `-jvm` suffix

Earlier versions of this rule told you to write `ai.koog:agents-mcp-jvm`. **That is no
longer correct.** At 1.2 the Gradle Module Metadata resolves the JVM variant from the
bare coordinate: use `ai.koog:agents-mcp:1.2.0-beta`. The `-jvm` artifacts still exist
for Maven consumers, who need them because Maven does not read Gradle metadata.

- Gradle → bare coordinate
- Maven → `-jvm` suffix (`koog-agents-jvm`, `agents-mcp-jvm`, …)

## Package locations that are not where you would guess

Verified by compiling against 1.2.0. Each of these produces an `Unresolved reference`
that looks like a missing dependency but is a wrong import.

| Symbol | Actual package | Notes |
|---|---|---|
| `subgraphWithTask`, `subgraphWithVerification`, `CriticResult` | `ai.koog.agents.ext.agent` | Ships **inside `agents-core`** (umbrella), NOT the standalone `agents-ext` artifact |
| `forwardTo` | — | A member of the strategy builder. **Do not import it**; importing fails |
| `fromProcess`, `defaultStdioTransport` | `ai.koog.agents.mcp` | Top-level **JVM-only extensions on the `McpToolRegistryProvider` object**. Import by name; importing only the provider does not bring them into scope |
| `McpServerInfo` | `ai.koog.agents.mcp.metadata` | Not `ai.koog.agents.mcp` |
| `TextDocument` | `ai.koog.rag.base` | An **interface** (`content`/`id`/`metadata`), not a data class. It has no constructor — implement it |
| `SimilaritySearchStrategy` | `ai.koog.agents.longtermmemory.retrieval.search` | Not under `rag.base.storage.search` |
| `ReadFileTool`, `ListDirectoryTool` | `ai.koog.agents.ext.tool.file` | In the standalone `agents-ext` beta artifact |
| `JVMFileSystemProvider` | `ai.koog.rag.base.files` | |
| tool event fields | — | `ToolCallStartingContext` exposes `toolName` / `toolArgs`, **not** `tool.name` |
| `ToolRegistry.tools` | — | Returns `List<ToolBase<*, *>>`, not `List<Tool<*, *>>` |

## `maxAgentIterations` lives on `AIAgentConfig`, not the factory

The `AIAgent(...)` overloads that take `systemPrompt` + `llmModel` have **no**
`maxAgentIterations` parameter. Passing one silently fails to match any overload and
the compiler then reports a cascade of unrelated errors inside the trailing lambda.

```kotlin
AIAgent(
    promptExecutor = executor,
    agentConfig = AIAgentConfig.withSystemPrompt(
        prompt = SYSTEM_PROMPT,
        llm = GoogleModels.Gemini3_5Flash,
        maxAgentIterations = 200,   // default is 3 — far too low for a verify/refine loop
    ),
    strategy = myStrategy,
    toolRegistry = registry,
) { /* features */ }
```

The default of **3** will abort any non-trivial graph. A verify → refine → verify loop
needs 100+.

## Bound every critic loop

`subgraphWithVerification` will reject indefinitely if the drafting phase cannot satisfy
it. That is an unrecoverable hang dressed as a safety feature, and it surfaces as
`AIAgentMaxNumberOfIterationsReachedException`. Count refusals and route to `nodeFinish`
after N:

```kotlin
val refusals = AtomicInteger(0)
edge(verify forwardTo refine
        onCondition { !it.successful && refusals.incrementAndGet() <= 2 }
        transformed { it.feedback })
edge(verify forwardTo nodeFinish
        onCondition { !it.successful }          // out of retries: ship the last draft
        transformed { it.input })
```

## JDK and tooling minima

- JDK 17 minimum. Gradle must *run* on a JDK its version supports — Gradle 8.4 on JDK 25
  is out of range; pin `org.gradle.java.home` or upgrade Gradle
- Kotlin **2.3.10 or later**. Earlier versions fail at consume time with
  `binary version of its metadata is 2.3.0, expected version is 2.1.0`
- Android consumers must set `android.useAndroidX=true`
