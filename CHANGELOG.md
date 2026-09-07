# Changelog

All notable changes to this plugin are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning: [SemVer](https://semver.org/).

## [0.5.0] — 2026-09-07

Retargets the plugin from Koog 1.0 to **Koog 1.2.0** (released 2026-08-28) and covers the
two surfaces added since 1.0. Findings below were produced by building a four-round
agent against 1.2.0 end to end; each coordinate and package path was verified by
compiling, not by reading release notes.

### Added

- `use-agent-skills` — the `skills` module Koog shipped in **1.2.0**, implementing the
  Agent Skills specification (agentskills.io): `discoverSkills` / `generateSkillsPrompt`,
  the SKILL.md frontmatter contract, the required `agents-ext` file tools, and
  disclose-before-apply so the tool trace shows which skill fired. Evals:
  `use-agent-skills-runtime-catalog`
- `use-cli-agents` — `CliAIAgent`, added in **1.1.1**: drives Claude Code / Codex / an
  arbitrary binary on subscription auth (`apiKey = null`) and composes via `.asNode()`.
  Covers the named-`generateRequest` trap, the workspace-scoping requirement, and
  fail-closed branching on the nullable `structuredResult`. Evals:
  `use-cli-agents-cross-vendor-critic`, `use-cli-agents-refuses-plain-llm-call`

### Changed

- `rules/module-coordinates.md` retargeted to 1.2 and restructured around the failure
  that actually costs build cycles — **two version lines**. The umbrella is `1.2.0`;
  MCP, CLI agents, skills, the Google client, `prompt-executor-llms-all`, long-term
  memory, `agents-ext` and `koog-agents-additions` publish **only** at `1.2.0-beta`.
  The rule now carries the coordinate table instead of prose
- **New:** documented that the umbrella does not bundle the Google provider. Gemini
  needs both `prompt-executor-google-client` and `prompt-executor-llms-all`, both beta.
  The symptom is `Unresolved reference 'google'` while `AIAgent` itself resolves, which
  reads as a broken install rather than a missing dependency
- **New:** a package-location table for symbols that are not where they look like they
  should be — `McpServerInfo` under `.metadata`, `TextDocument` as an *interface* in
  `ai.koog.rag.base` with no constructor, `SimilaritySearchStrategy` under
  `longtermmemory.retrieval.search`, the file tools in `agents-ext`, `toolName` (not
  `tool.name`) on tool events, `ToolRegistry.tools` returning `ToolBase`, and
  `forwardTo` being a builder member that must **not** be imported
- **New:** `rules/agent-construction.md` now documents the iteration cap honestly. It is
  one underlying value, `AIAgentConfig.maxAgentIterations`, reached under two different
  parameter names. The convenience `AIAgent(...)` overloads expose it as `maxIterations`
  and default it to **50**; the `agentConfig` overloads expose no cap at all; and
  `AIAgentConfig.withSystemPrompt(...)` defaults `maxAgentIterations` to **3**. Passing
  the name `maxAgentIterations` to a convenience overload matches nothing, and the
  compiler then reports a cascade of unrelated errors inside the trailing lambda, which
  sends you hunting in the wrong file. Verified against the 1.2.0 sources of
  `AIAgentFactory.kt` and `AIAgentConfig.kt`
- **New:** `author-strategy` now bounds its verify/fix loop. `subgraphWithVerification`
  will reject indefinitely when the drafting phase cannot satisfy it, surfacing as
  `AIAgentMaxNumberOfIterationsReachedException` — an unrecoverable hang wearing a
  safety feature's clothes. The exhaustion edge terminates with an explicit unapproved
  outcome rather than shipping the last draft as if it had passed; a critic whose
  rejection is indistinguishable from an approval is decorative

### Fixed

- Removed the `-jvm` suffix guidance for Gradle consumers. At 1.2 the bare coordinate
  (`ai.koog:agents-mcp:1.2.0-beta`) resolves the JVM variant through Gradle Module
  Metadata; the suffix is now Maven-only. The old rule sent Gradle users to an artifact
  they did not need
- Plugin summary and every skill description now say Koog 1.2 rather than Koog 1.0
- `skills/wire-mcp-server` still pinned MCP at `1.0.0-beta` and still demanded the `-jvm`
  suffix, in both Step 2 and the server section — directly contradicting the retargeted
  coordinate rule three files away. An agent loading both got opposite instructions
- `README.md` labelled its authority link "Koog 1.2.0 source" while pointing at
  `tree/1.0.0`, and cited the v1.0.0 release notes. Consumers resolving an API
  disagreement were sent to the wrong release. Both targets now resolve to 1.2.0
- `README.md`'s `module-coordinates` summary row still described the 1.0 contract
  (`1.0+`, `-jvm` suffix). It now matches the rule it summarizes
- `README.md` skills tables gained the two new rows; `use-agent-skills` and
  `use-cli-agents` shipped in the manifest but were invisible on the entrypoint
- `Step 0` in both new skills redirected unsuitable requests to another skill and then
  fell through into installing the dependency anyway. Both redirects are now terminal
- `add-tool`'s description referred to `scaffold-agent` in prose instead of a typed
  `Skill(skill: ...)` call — the only prose cross-skill reference left in the plugin
- `migrate-from-0-x` told readers to set every `ai.koog:*` artifact to one version
  string, which cannot work across two version lines

### Evals

Partial run — **20 of 50 scenarios**, on the publish-time default solver
`claude:deepseek-v4-flash`. The workspace ran out of credit mid-run, so this is a
subset, not a suite result, and it is not comparable to the whole-suite numbers in
earlier releases.

- **75% with the plugin against 34% without**, across the 20 scenarios that scored both
  arms. Mean rubric points 14.0 against 5.4, mean lift **+8.6**
- Largest lifts land where the plugin does its actual work — routing a request to the
  right primitive: `persist-chat-history-refuses-fact-store` +25,
  `persist-chat-history-jdbc` +20, `use-llm-node-variants-streaming` +20,
  `snapshot-and-restore-refuses-crash` +19
- Near-zero lift on stable-API scenarios (`add-observability-langfuse`,
  `manage-state-tldr-mid-phase`, both +0) is the expected shape: a competent model
  already writes correct Koog for APIs that did not change. Not a scenario defect
- Two negatives, `migrate-from-0-x-agentmemory-removal` −14 and
  `use-attachments-image-input` −11, both with a plugin-arm score of 0. That is the
  signature of the harness stub failure (solution directory left pristine), not a
  regression. Re-run before reading anything into them

**The three scenarios added in this release are unmeasured.**
`use-agent-skills-runtime-catalog` and `use-cli-agents-cross-vendor-critic` never scored
either arm; `use-cli-agents-refuses-plain-llm-call` scored 20/20 on the plugin arm but
its baseline never ran, so there is no lift figure. 0.5.0's new surface has not been
evaluated — treat the headline as covering the pre-existing skills only.

### Manifest and terminology

- Migrated `tile.json` to `.tessl-plugin/plugin.json` via `tessl plugin migrate`. The
  publish workflow's path filter and display name follow it. `keywords` and `entrypoint`
  were carried across by hand — the migration drops them
- "Tile" is retired throughout in favour of **agentic context plugin**. Renamed the
  repository to `jbaruch/koog-plugin`. Literal `tessl` CLI invocations, registry URLs,
  and the badge endpoint are unchanged: those are commands and addresses, not
  terminology

### Notes moved off the loaded surfaces

Detail trimmed from skills and rules under `context-writing-style`'s What to Cut, kept
here because it is the evidence behind the directives:

- CLI agent latency, measured against 1.2.0-beta on one prompt: `claude` ~11s, `codex`
  ~38s, `grok` ~66s, against ~2-3s for a direct API call. This is the number behind
  "one to two orders of magnitude slower" and the 10-60s step budget
- Why `use-cli-agents` insists on a scratch workspace: asked only to emit a JSON object,
  a real CLI listed the project directory, found a spec file, read it, and used its
  contents to get the answer right. Impressive, and also a pipeline stage silently
  taking a dependency on whatever files happen to be nearby
- The `-jvm` rule reversed between 0.4.x and 0.5.0. Earlier versions of
  `module-coordinates` instructed `ai.koog:agents-mcp-jvm`; at 1.2 Gradle Module
  Metadata resolves the JVM variant from the bare coordinate, and the suffix is needed
  only by Maven, which does not read that metadata
- `rules/module-coordinates.md` no longer carries Kotlin examples. Rule Format allows
  code blocks only for specific commands, so the iteration example moved into
  `rules/agent-construction.md` as prose and the critic loop into `author-strategy`

## [0.4.10] — 2026-05-31

### Fixed

- `use-planner` Steps 2-3 — added the `Path:` file-write convention so the `ai.koog:agents-planner` dependency and the agent construction land in graded `build.gradle.kts` / `Main.kt` files instead of inline prose. The 3-run eval showed `use-planner-llm-based-triage` reliably missing "adds the separate planner module dependency" (33%) because the dependency was never written to a file
- `use-planner` Step 1 — the graph-DSL redirect now adds a top-of-file comment acknowledging the developer's "planning" wording. The `use-planner-refuses-when-graph-fits` scenario scored 16% on "acknowledges the developer's framing without capitulating" because the redirect named the topology and round-trips but never engaged the "planning" word

## [0.4.9] — 2026-05-30

### Fixed

- `add-structured-output` Steps 1-2 — added the `Path:` file-write convention (the same fix `use-llm-node-variants` got in 0.4.6). Both actions now write `Main.kt` (plus `Strategy.kt` for Step 2) and `build.gradle.kts` to disk instead of emitting inline code the eval scorer can't see. The eval `add-structured-output-classify-issue` had scored 0/0 in every run — baseline and with-context — because the file-reading scorer saw an empty solution directory. This was the last skill missing the `Path:` convention from the 0.4.4 rollout
- `evals/add-structured-output-classify-issue` — Output Specification reworded to a need-only description (update the project so `agent.run(...)` returns `IssueClassification` directly). The file-write convention lives in the skill's `Path:` directives, not the task, so the baseline does not get the technique (`plugin-evals` No Bleeding)
- Refuse/redirect eval criteria — the "does NOT do X" criteria in seven scenarios (`domain-model-subtask-pipeline-refuse`, `cache-llm-calls-refuses-provider-side`, `persist-chat-history-refuses-fact-store`, `snapshot-and-restore-refuses-crash`, `use-planner-refuses-when-graph-fits`, `add-persistence-refuses-conversation`, `scaffold-agent-refuse-non-empty-dir`) now explicitly fail on an empty/missing solution. A stubbed (no-output) run previously passed them vacuously, scoring 35-60/100 on a non-answer and masquerading as negative lift in the suite

## [0.4.8] — 2026-05-29

### Fixed

- `snapshot-and-restore` Step 1 + `add-persistence` Step 2 — completed the crash-recovery redirect handoff. The snapshot Step 1 redirect previously said only "invoke add-persistence", so with the skill loaded the agent emitted a meta-description of the skill chain instead of the concrete `install(Persistence)` solution and a developer-facing message — eval `snapshot-and-restore-refuses-crash` regressed to with-context 42 vs baseline 66. The redirect now directs the agent to deliver the full Persistence solution plus a one-message snapshot-vs-Persistence mismatch explanation, and `add-persistence` Step 2 gained checkpoint-frequency cost guidance (every-step writes are expensive on long runs). With-context returned to 100

### Added

- `evals/migrate-from-0-x-custom-strategy` — second migration scenario covering the non-obvious 1.0 breaking changes a custom-strategy agent hits: the `nodeExecuteTools` auto-writeback removal (chain `nodeLLMSendToolResults` explicitly), the `LLMClient` HTTP-transport decoupling (`KoogHttpClient.Factory` instead of a Ktor `HttpClient`), and the `kotlin.time.Clock` → `KoogClock` swap, alongside the coordinate/JDK bumps
- `evals/wire-acp-server-choose-vs-a2a` — protocol-selection scenario: a tooling dashboard needing run-lifecycle control with cancellation and progress streaming should pick ACP (`agents-features-acp`), not A2A (agent-to-agent RPC) or MCP (tool host). Highest-lift new scenario (+95 baseline→with-context)

### Changed

- `evals/add-structured-output-classify-issue` — removed the answer-narrating clause from the task so the "does not introduce a custom strategy" criterion tests application rather than reading

## [0.4.7] — 2026-05-29

### Changed

Knowledge corrections from Vadim Briliantov (Koog project lead) — three skill clarifications:

- `wire-mcp-server` Step 6 — added a framing paragraph stating that `@Tool` / `@LLMDescription` / `ToolSet` are **LOCAL** Koog tool annotations. The `startStdioMcpServer` path bridges a Koog `ToolRegistry` to MCP, but that's a secondary use case for the annotation, not its primary purpose. For projects whose primary goal is publishing tools over MCP (independent of any Koog agent), the Kotlin MCP SDK (`io.modelcontextprotocol:kotlin-sdk`) has its own server annotation. The Koog-bridge path is right when you already have a Koog `ToolRegistry` and want it reachable over MCP too
- `add-tool` Step 3 (Sub-Agent-as-Tool) — added the sub-agent vs subgraph distinction. Sub-agents (`AIAgentService.fromAgent`) are fully independent agents that communicate only through typed input/output; subgraphs (`subgraphWithTask` / `subgraphWithVerification`) are part of the same agent and share one message history. Default for "break my agent into stages" is subgraph; reach for sub-agent only when isolation is the explicit requirement
- `domain-model-subtask-pipeline` Step 6 — strengthened the auto-shared-history framing to name the contrast with independent-agent abstractions (Koog sub-agents, LangChain4j Agentic sub-agents). Subgraphs live on one common history; independent agents communicate only through typed input/output. Cross-references `Skill(skill: "add-tool")` Step 3 for the isolation case

## [0.4.6] — 2026-05-28

### Fixed

- `use-llm-node-variants` Steps 1-4 — added the `Path:` write directive to each action (streaming / multiple-choice / moderation / force-one-tool). Eval `019e648a` against 0.4.5 surfaced that `use-llm-node-variants-streaming` regressed to lift -87 (baseline 87 → with-context 0) with the reasoning "No Kotlin code was produced at all". Same file-write-gap root cause as the 0.3.1 nightmare — this was the one skill that hadn't been patched in PR #10's omnibus `Path:` rollout
- `evals/persist-chat-history-refuses-fact-store/criteria.json` — re-weighted to favor functional correctness over prose explanation. The 0.4.5 eval showed the agent did the right thing functionally (refused JdbcChatHistory, picked LongTermMemory) but lost 35/100 on prose-explanation criteria (30 + 5) because the patched skills explicitly direct code-only output via `Path:`. New weights: Does not install JdbcChatHistory 35 (was 25), Recommends LongTermMemory 30 (was 25), Does not synthesise pseudo-turns 25 (was 15), Names the distinction 10 (was 30, reworded to accept a code comment), and dropped the standalone "Acknowledges the framing" criterion (was 5). Sum still 100

## [0.4.5] — 2026-05-26

### Added

- `wire-mcp-server` Step 6 — optional server-side `startStdioMcpServer` flow for users authoring an MCP server (not just consuming one). Pulls `ai.koog:agents-mcp-server-jvm:1.0.0-beta` (same beta + `-jvm`-suffix gotchas as the client module). Exposes a `ToolRegistry` over stdio with `awaitCancellation()` keeping the process alive
- `domain-model-subtask-pipeline` Step 7 — `MultiLLMPromptExecutor` callout for cross-provider per-subgraph model selection. The skill teaches per-phase `llmModel = ...` but the per-provider executors (`simpleOpenAIExecutor` / `simpleAnthropicExecutor`) only know one provider; when a strategy mixes (e.g., `OpenAIModels.Chat.O3` for the verifier and `AnthropicModels.Sonnet_4_5` for the deployer), the agent needs a `MultiLLMPromptExecutor` with one client per provider
- `persist-chat-history` Step 5 — multi-turn footnote: the same agent instance can call `agent.run(input, sessionId = ...)` repeatedly; the installed chat-history backend accumulates the message log on each call, so a `while (true)` driver loop maintains conversation without reconstructing the agent
- `persist-chat-history` Step 6 — new anti-pattern section: **don't use chat-history as a fact store**. Symptoms (date-prefixed pseudo-turns, synthetic `Message.Assistant` claims about actions the agent never took, queryable structured data forced into a sequential channel) and the right primitives for each shape (`LongTermMemory` for cross-session facts; `@Tool` for queryable structured data; `systemPrompt` for small fixed context; `storage` for run-scoped state). Custom `ChatHistoryProvider` stays legitimate for replaying real conversation messages from external sources
- `add-observability` Step 3 — one-line clarification on `setVerbose(true)` semantics: it emits prompts, completions, and token counts on each span
- `evals/wire-mcp-server-author-stdio` — positive scenario covering the new Step 6 decisional branch (publishing an existing `ToolSet` as a stdio MCP server). Criteria check for `startStdioMcpServer`, the `agents-mcp-server-jvm:1.0.0-beta` dependency, `ToolRegistry { tools(asTools()) }` reuse of the developer's existing class, `awaitCancellation()` for process lifetime, and refusal of the client-transport surface
- `evals/persist-chat-history-refuses-fact-store` — negative scenario covering the new Step 6 anti-pattern (don't use chat-history as a fact store). Criteria check that the agent names the chat-history-vs-fact-store distinction, recommends `LongTermMemory` or a `@Tool`, refuses to install `JdbcChatHistory` / `ChatHistoryAws` / `ChatMemorySql`, and refuses to synthesise pseudo-turns in a custom `ChatHistoryProvider`

## [0.4.4] — 2026-05-26

### Changed

- Hardened skills against the **file-write** failure mode observed in 0.3.1 eval run `019e60f5` and confirmed in the partial re-run `019e613e`: the scorer reads files from the solution directory, but several skills told the agent "produce ... as part of your response" — which the agent satisfied with stdout prose that the scorer can't see. Adopted the `Path:` convention from `scaffold-agent` across the patched skills so the file targets are explicit and unambiguous (full `src/main/kotlin/com/example/<file>` paths plus `build.gradle.kts` at repo root):
  - `add-observability` Step 3 — `Path: src/main/kotlin/com/example/Main.kt` for the modified agent construction, `Path: build.gradle.kts` for the dependency
  - `manage-state` Step 2 — `Path: src/main/kotlin/com/example/Strategy.kt` for the boundary-node body
  - `persist-chat-history` Step 4 — `Path: src/main/kotlin/com/example/Main.kt` + `Path: build.gradle.kts` + a concrete handler path (e.g., `src/main/kotlin/com/example/Routes.kt`) when the user named a handler
  - `add-tool` Step 2 — `Path: src/main/kotlin/com/example/AccountLookupTool.kt` (concrete tool-name example, rename to match the actual tool) + `Path: src/main/kotlin/com/example/Main.kt` + `Path: build.gradle.kts`
  - `author-strategy` Step 8 — `Path: src/main/kotlin/com/example/Strategy.kt` for the DSL + `Path: src/main/kotlin/com/example/Main.kt` for the modified construction
  - `handle-agent-events` Step 2 — `Path: src/main/kotlin/com/example/Main.kt` + `Path: build.gradle.kts`
  - `wire-ktor-server` Step 5 — `Path: src/main/kotlin/com/example/Application.kt` + `Path: build.gradle.kts`

- `add-tool` Step 1 routing — annotated-tool default now defers to Step 2 (typed `Tool<TArgs,TResult>`) when the user's existing function takes a `data class` parameter or returns a typed result; the previous "default to Step 1" rule wrapped typed signatures in flat-primitive annotated tools and lost the type contract

- `handle-agent-events` Step 2 — adopted the same `Path:` directive; round-2 eval `019e6149` showed the prior prose-only handoff was non-deterministic (round 1: 100, round 2: 0)

- `use-planner` Step 1 redirect to `author-strategy` — made the redirect actionable: it now runs `author-strategy` end-to-end and writes the graph DSL code via `Path:` per author-strategy's Step 8, plus topology and round-trip-cost reasoning as comments at the top of the produced file; the previous wording let the agent stop at a prose explanation. Step 1 also adds a "Finish here — do not continue into planner-variant selection or Step 2 / Step 3" line so the redirect and planner-fits branches are mutually exclusive, and an explicit "Chaining exception (exhaustive — overrides 'Do not run other steps' only as listed)" preamble names the Step 1 → Step 2/3 chain per `skill-authoring`

- Hardened skills against the "no-output" failure mode observed in 0.3.1 eval run `019e60f5`:
  - `add-observability` Step 2 — replaced blocking "Ask the user which backend" with a non-blocking pick + OTLP default; Step 3 writes via `Path:`; fixes the −100pp lift on `add-observability-langfuse`
  - `wire-ktor-server` Step 2 — split into minimal install (mandatory) vs MCP/HOCON add-ons; Steps 3 and 4 each open with "Skip this step entirely if..."; Step 5 writes via `Path:`; fixes the −28pp lift on `wire-ktor-server-route`
  - `manage-state` Step 2 — committed to `HistoryCompressionStrategy.WholeHistory` as the default and moved the other six variants into "use only when the user names them"; fixes the −80pp lift on `manage-state-tldr-mid-phase`
  - `use-planner` Step 1 — converted the "ask user LLM-based vs GOAP" stall into a pick-by-keywords rule (GOAP only when the user names typed state / classical planner / state space); planner-redirect tasks no longer block on a clarifying question
- Tightened skill activation routing for planner construction:
  - `use-planner` description now names `Planners.llmBased`, `Planners.llmBasedWithCritic`, `Planners.goap`, `PlannerAIAgent`, `agents-planner`
  - `scaffold-agent` description adds an explicit "Do NOT use when the user is constructing a planner / picking a strategy / naming a specific agent shape" exclusion; fixes the mis-activation that pulled `scaffold-agent` for `use-planner-llm-based-triage`

### Removed

- `cache-llm-calls-redis-shared` eval scenario — retired per `plugin-evals.md` "Lift, Not Attainment": baseline 100/100, lift 0pp (Cause #1, universal competence). The partner scenario `cache-llm-calls-refuses-provider-side` still covers `cache-llm-calls` at +100pp lift

### Fixed

- `handle-agent-events-stdout-trace` task — stripped the "arrow indicating start vs end" framing that bled into the criterion "Uses distinct visual markers for start vs end" (`plugin-evals.md` "No Bleeding")
- `add-tool-typed-args-with-result` task — stripped "they want the tool's input and output to remain these typed shapes — not a JSON blob, not a flattened String" framing that telegraphed `Tool<TArgs,TResult>`; the typed `queryAccount` signature still carries the constraint
- `persist-chat-history-jdbc` task — replaced "wants conversations to persist by user account" framing with a user-reported bug ("the bot doesn't remember anything we talked about") so the agent must navigate persistence-feature vs chat-history vs LongTermMemory on its own

## [0.4.3] — 2026-05-26

### Fixed

- Stripped XML-tag-looking syntax from skill `description:` fields in `add-tool` (`Tool<TArgs,TResult>` → `Tool[TArgs,TResult]`; `<X>` / `<function>` → prose), `domain-model-subtask-pipeline` (`subgraphWithTask<In, Out>` → `subgraphWithTask[In, Out]`; same for `subgraphWithVerification<T>` and `CriticResult<T>`), `use-llm-node-variants` (`<tool>` → prose). The `tessl skill review` validator rejects `<` followed by alpha as an XML tag — the just-landed skill-review CI gate (0.4.1) failed `add-tool` on the previous 0.4.2 publish attempt because of this. **The 0.4.2 publish never landed in the registry** (failed at the gate); 0.4.3 ships the same content as 0.4.2 plus this descriptor fix

## [0.4.2] — 2026-05-26 (never published)

### Fixed

- `author-strategy` skill — added a member-vs-extension import table at the end of Step 5. The DSL primitives split across two shapes: `forwardTo` / `onCondition` / `transformed` are infix members (no import needed); `onToolCalls` / `onTextMessage` / `onIsInstance` / `onSuccessful` / `onFailure` / `asUserMessage` / `asToolResultMessage` / `onMessageParts` are top-level extensions in `ai.koog.agents.core.dsl.extension.*` (each needs its own import). Inventing a member import or omitting an extension import is the most common copy-paste compile failure
- `author-strategy` + `domain-model-subtask-pipeline` skills — fixed the wrong artifact text. `subgraphWithTask` / `subgraphWithVerification` / `CriticResult` (package `ai.koog.agents.ext.agent`) ship inside `agents-core` (which `koog-agents` umbrella pulls), NOT the standalone `ai.koog:agents-ext:1.0.0-beta` artifact. Added the imports + the `AIAgentGraphStrategy` package note (`ai.koog.agents.core.agent.entity`, not bare `ai.koog.agents.core.agent`)
- `wire-mcp-server` skill — each transport builder (`streamableHttp`, `fromSseUrl`, `fromProcess`) is a top-level extension in `ai.koog.agents.mcp` declared separately from the `McpToolRegistryProvider` object. All three example blocks now show the explicit extension import alongside the provider import
- `wire-mcp-server` skill + `module-coordinates` rule — corrected the MCP client dependency to `ai.koog:agents-mcp-jvm:1.0.0-beta`. Koog 1.0 stable did not publish `agents-mcp` / `agents-mcp-server` at `1.0.0`; only `1.0.0-beta` is on Maven Central, and they publish only JVM variants so the `-jvm` suffix is mandatory for Gradle KMP variant resolution
- `add-tool` + `wire-a2a` skills — corrected the `ToolSet` and `asTools` imports to come from `ai.koog.agents.core.tools.reflect.*` (the actual package), not bare `ai.koog.agents.core.tools.*`
- `module-coordinates` rule — added Kotlin 2.3.10+ minimum requirement. Koog 1.0 is compiled with Kotlin 2.3.x; earlier Kotlin versions fail at consume time with metadata-version errors
- `evals/domain-model-subtask-pipeline-triage/criteria.json` — corrected C7's required-Gradle-deps description: the umbrella `ai.koog:koog-agents:1.0.0` is sufficient (it pulls `agents-core`, which contains the subgraph DSL). Penalize unnecessary `agents-ext` lines

### Added

- `evals/author-strategy-import-shapes` — new positive scenario testing the member-vs-extension import correctness when the agent emits a tool-handling-loop strategy
- `evals/wire-mcp-server-import-shapes` — new positive scenario testing the `fromSseUrl` extension import + the `-jvm:1.0.0-beta` dependency line. Updated `wire-mcp-server-stdio-playwright`, `wire-mcp-server-streamable-http`, and `wire-mcp-server-merge-tools` criteria to match the corrected artifact spec

Closes #9 (items 1–9; item 10 is a separate Tessl install-policy investigation).

## [0.4.1] — 2026-05-26

### Added

- `.github/workflows/publish.yml` — `tessl skill review --threshold 85` gate before `tessl tile publish .` via `jbaruch/coding-policy/.github/actions/skill-review@ef67ffe5` (changed-skills loop). Closes the `context-artifacts` Mandatory Review gap flagged on #7; below-threshold skill scores now block publish. Checkout step bumped to `fetch-depth: 0` so the action's `git diff $github.event.before..HEAD` can resolve the prior commit. Closes #8

## [0.4.0] — 2026-05-25

### Added

- `domain-model-subtask-pipeline` skill — the integrated pattern for typed-handoff pipelines: tools sliced by access into separate `ToolSet`s (read / write / communication), `@Serializable` `@LLMDescription`-annotated data classes as inter-subtask contracts, `subgraphWithTask<In, Out>` per phase with per-phase model selection, `subgraphWithVerification<T>` + `CriticResult<T>` for self-correction loops. The methodology JetBrains' KotlinConf 2026 banking demo demonstrates — fills the gap left by `author-strategy` (DSL mechanics only) and `add-structured-output` (top-level typed output only)
- 2 eval scenarios — `domain-model-subtask-pipeline-triage` (positive, four-phase support workflow) and `domain-model-subtask-pipeline-refuse` (negative — declines to over-engineer a one-shot text transform)

## [0.3.1] — 2026-05-25

### Added

- `.github/workflows/publish.yml` — `tesslio/patch-version-publish@v1` wired to push-on-main + manual dispatch; auto-bumps patch from the registry on future merges
- `.github/workflows/review-openai.md` / `review-anthropic.md` — paired gh-aw PR reviewers from `jbaruch/coding-policy: install-reviewer` (cross-family review per `author-model-declaration`)
- `.env.example` — required hosted-CI secrets with placeholder values and a deep link to `https://github.com/jbaruch/koog-tessl/settings/secrets/actions` (per `no-secrets`)
- `.pre-commit-config.yaml` — gitleaks v8.21.2 + standard pre-commit-hooks (per `no-secrets` pre-commit-scanning requirement)
- 4 negative eval scenarios — `use-planner-refuses-when-graph-fits`, `cache-llm-calls-refuses-provider-side`, `add-persistence-refuses-conversation`, `snapshot-and-restore-refuses-crash` — covering the cross-skill redirects each skill prescribes (closes the "only positive cases" gap from 0.3.0)
- 1 generator-produced eval scenario — `use-attachments-pdf-and-url` (PDF + URL-image attachments in a single LLM call); the existing `use-attachments-image-input` only exercised images
- `scenario.json` backfilled on every eval scenario (40 total) to match the canonical three-file shape `tessl scenario generate` emits — drift fix, not a feature

### Changed

- Slimmed `tile.json` `summary` from a 290-char comma-spliced multi-clause string back to a one-line description per `skill-authoring.md`
- Stripped `interaction-rules` phantom reference from 8 skill files (the rule never existed in this tile or in any consumed tile)
- Converted 7 prose skill cross-references (`see the \`X\` skill`, `redirect to \`X\``) to typed `Skill(skill: "X")` calls per `skill-authoring.md` "Typed Calls"
- Split 6 step titles that combined verbs with "and" (`Verify and Hand Off`, `Bump JDK and Tooling`, etc.) per `skill-authoring.md` "Step Structure"
- Reworded `model-planner-subtasks-parallel-tree` task to remove technique leak (`PlannerNode composition`, `storage-key tracking`) per `plugin-evals.md` "No Bleeding"
- Reworded `use-attachments-pdf-and-url` task on the cross-family reviewer's finding — dropped `strategy` / `user message` technique prose
- Renamed `enable-prompt-caching-anthropic-long-system` (43 chars) → `enable-prompt-caching-anthropic` (31) to fit the 40-char default cap
- Renamed `wire-mcp-{merge-with-existing-tools,stdio-playwright,streamable-http-github}` → `wire-mcp-server-{merge-tools,stdio-playwright,streamable-http}` so prefixes match the skill name per `plugin-evals.md` "Naming"

## [0.3.0] — 2026-05-25

### Added

- 7 additional skills covering modules and API surfaces missed in 0.2.0:
  - `cache-llm-calls` — in-process LLM-response cache (`prompt-executor-cached` + `prompt-cache-{files,model,redis}`), distinct from the provider-side caching covered by `enable-prompt-caching`
  - `persist-chat-history` — chat-history persistence backends (`chat-history-jdbc`, `chat-history-aws`, `chat-memory-sql`), distinct from generic persistence and `LongTermMemory`
  - `test-koog-agents` — deterministic agent testing with `agents-test` (scripted executor, fake `KoogClock`, event-handler recorder)
  - `trace-agent-internals` — deep diagnostic trace feature (`agents-features-trace`), distinct from OpenTelemetry (production signal) and event handlers (high-level callbacks)
  - `query-sql-from-agent` — SQL-querying feature (`agents-features-sql`) with read-only mode, schema scoping, row caps
  - `model-planner-subtasks` — `PlannerNode` tree composition, parallel vs sequential subtasks, retry-on-parse-failure edges, history compression between phases
  - `use-functional-agent` — `FunctionalAIAgent` (the third concrete agent subtype, alongside `GraphAIAgent` and `PlannerAIAgent`) — single suspending block, no graph
- 7 new eval scenarios — 1 per new skill, weighted-checklist with non-uniform weights summing to 100
- Scope statement in README clarified: Kotlin/JVM only; Kotlin/JS, Kotlin/Native, Compose Multiplatform explicitly out of scope

## [0.2.0] — 2026-05-25

### Changed (breaking)

- **Slimmed rules from 9 to 2.** Only `module-coordinates` and `agent-construction` remain always-on — they cover gotchas every Koog project hits. The other 7 rules were converted to on-demand skills:
  - `strategy-dsl` → `author-strategy` skill
  - `planner-vs-graph` → `use-planner` skill
  - `tools-and-mcp` → folded into `add-tool` and `wire-mcp-server` skills (already existed)
  - `state-and-memory` → `manage-state` skill
  - `observability` → `add-observability` skill
  - `spring-boot-integration` → `wire-spring-boot` skill
  - `migration-from-0-x` → `migrate-from-0-x` skill
- Front-loaded token cost dropped from ~6.9k to ~1.3k

### Added

- 19 new skills filling Koog 1.0 surface gaps not covered in 0.1.0:
  - High priority: `add-structured-output`, `define-prompt`, `add-persistence`
  - Medium priority: `enable-prompt-caching`, `handle-agent-events`, `wire-ktor-server`
  - Lower priority but real coverage: `use-llm-node-variants` (streaming / multiple-choices / moderation / force-one-tool), `add-rag`, `wire-a2a`, `wire-acp-server`, `add-token-budgeting`, `snapshot-and-restore`, `use-attachments`
- 19 new eval scenarios — 1 per new skill, all weighted-checklist with non-uniform weights summing to 100
- `agent-construction` rule now includes a "When to reach for a skill" index pointing to the right skill for each common task

## [0.1.0] — 2026-05-25

### Added

- Initial tile targeting Koog 1.0.0 (released 2026-05-21)
- 9 always-apply rules covering module coordinates, agent construction, the strategy DSL, planner vs graph, tools & MCP, state & memory, observability, Spring Boot integration, and the 0.x → 1.0 migration surface
- 3 skills: `scaffold-agent`, `add-tool`, `wire-mcp-server`
- 9 eval scenarios (3 per skill)
- Kotlin-only scope; Java-interop surface deferred to a future sibling tile
