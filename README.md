# jbaruch/koog

[![tessl](https://img.shields.io/endpoint?url=https%3A%2F%2Fapi.tessl.io%2Fv1%2Fbadges%2Fjbaruch%2Fkoog)](https://tessl.io/registry/jbaruch/koog)

Koog 1.2 idioms, gotchas, and skills for Kotlin agents on the JVM.

Koog reached 1.0 on 2026-05-21. The published documentation at `docs.koog.ai` lags the 1.0 source in several places (Maven coordinates still listed as 0.7.1, MCP pages 404, no mention of the planner module split or HTTP transport decoupling). This tile codifies what the 1.0 source actually shows.

## Install

```bash
tessl install jbaruch/koog
```

## How this tile is shaped

Two **always-on rules** carry the gotchas every Koog project hits — module coordinates and agent construction. Everything else is an **on-demand skill** — load only when you reach for that surface.

## Rules

| Rule | Concept |
|---|---|
| `module-coordinates` | Pin against `ai.koog:*:1.0+` (with `1.0.0-beta` + `-jvm` suffix for the MCP and standalone-ext satellites); pull planner / MCP / Spring / Ktor modules explicitly; JDK 17 + Kotlin 2.3.10 minimum |
| `agent-construction` | Top-level `AIAgent(...)` factory; `installFeatures` trailing lambda; `singleRunStrategy()` default; `AgentMemory` removed |

## Skills

### Foundations
| Skill | What it does |
|---|---|
| `scaffold-agent` | Bootstrap a new Koog 1.2 Kotlin project from scratch |
| `add-tool` | Add a tool — annotated `@Tool` / typed `Tool<TArgs,TResult>` / sub-agent-as-tool |
| `define-prompt` | Author prompts beyond `systemPrompt = "..."` — DSL, few-shot, augmenters |
| `use-functional-agent` | Use `FunctionalAIAgent` — single suspending block, no graph, no planner |

### Orchestration
| Skill | What it does |
|---|---|
| `author-strategy` | Custom graph strategy with subgraphs and verify/fix loops |
| `domain-model-subtask-pipeline` | Integrated pattern — tools sliced by access, typed `@LLMDescription` handoff classes, `subgraphWithTask<In, Out>` chained by compile-time types |
| `use-planner` | LLM-based planner or GOAP when topology depends on runtime context |
| `model-planner-subtasks` | Planner's tree of subtasks — `PlannerNode` composition, parallel vs sequential, retries |
| `use-llm-node-variants` | Streaming, multiple-choice, moderation, force-one-tool nodes |
| `add-structured-output` | Typed JSON output via `responseProcessor` or `nodeLLMRequestStructured` |

### State and durability
| Skill | What it does |
|---|---|
| `manage-state` | `AIAgentStorage`, history compression, `LongTermMemory` (replaces removed `AgentMemory`) |
| `add-persistence` | Continuous checkpointing + `runFromCheckpoint` for crash resilience |
| `snapshot-and-restore` | Caller-triggered save points for explicit fork/replay |
| `persist-chat-history` | Chat-history backends (JDBC, AWS, SQL-typed) — resume conversations by session |

### Integrations
| Skill | What it does |
|---|---|
| `wire-mcp-server` | Connect to an MCP server — Streamable HTTP / SSE / stdio |
| `wire-spring-boot` | Spring Boot autoconfig — `ai.koog.<provider>.*` keys + `MultiLLMAutoConfiguration` |
| `wire-ktor-server` | Ktor plugin — agent over HTTP, MCP-block config |
| `wire-a2a` | Agent-to-Agent protocol — serve or consume |
| `wire-acp-server` | Agent Client Protocol — fine-grained client control with cancellation |
| `query-sql-from-agent` | SQL-querying feature with read-only mode, schema scoping, row caps |

### Quality and cost
| Skill | What it does |
|---|---|
| `add-observability` | OpenTelemetry feature — Langfuse / OTLP / Datadog / Weave backends |
| `handle-agent-events` | Per-step event handlers for human-readable trace surfaces |
| `trace-agent-internals` | Deep diagnostic trace — node entries, edge predicate evaluations, planner decisions |
| `enable-prompt-caching` | Anthropic prompt caching — automatic + explicit `cacheControl` breakpoints |
| `cache-llm-calls` | In-process LLM-response cache (in-memory, file, Redis) — distinct from provider-side caching |
| `add-token-budgeting` | Hard run budget + soft history compression on overrun |
| `test-koog-agents` | Deterministic testing — scripted executor, fake clock, event-handler recorder |

### Advanced
| Skill | What it does |
|---|---|
| `add-rag` | Embedder + vector store + retrieval as tool or augmenter |
| `use-attachments` | Multimodal — images, files, audio in user turns |

### Migration
| Skill | What it does |
|---|---|
| `migrate-from-0-x` | Bump a 0.x codebase to 1.0 — 14-step checklist covering every breaking change |

## Scope

This tile teaches **Kotlin** consumption of Koog 1.2 **on the JVM**. Java-interop surface (`AIAgentService`, `*Blocking` variants from #2005) is deferred to a future `jbaruch/koog-java`. Other Kotlin targets (Kotlin/JS, Kotlin/Native, Compose Multiplatform) are not covered — the rules and skills assume `kotlin("jvm")`, JDK 17, and JVM-only features (JVM shutdown hooks for OpenTelemetry, JDBC for persistence backends).

## Source of authority

Where this tile and `docs.koog.ai` disagree, this tile follows the [Koog 1.2.0 source](https://github.com/JetBrains/koog/tree/1.0.0) and the [v1.0.0 release notes](https://github.com/JetBrains/koog/releases/tag/1.0.0).
