---
name: use-agent-skills
description: >
  Give a Koog 1.2 agent capability bundles it discovers from the filesystem at
  runtime, using the `skills` module that implements the Agent Skills
  specification (agentskills.io). Discovers SKILL.md files, generates a catalog
  prompt block, and registers the file tools the agent needs to disclose and
  apply them. Use when the user asks to "add agent skills", "use the Agent
  Skills spec", "let the agent discover capabilities", "load skills from a
  directory", "add a SKILL.md", or names `discoverSkills` /
  `generateSkillsPrompt`. New in Koog 1.2.0 — not available on 1.0 or 1.1.
---

# Use Agent Skills Skill

Process steps in order. Do not skip ahead.

## Step 0 — Confirm this is the right tool

Agent Skills are **runtime-discovered capability bundles**, read off disk on every
run. Reach for them when the set of capabilities should change without recompiling —
a directory a non-developer drops files into, a skills repo shared across agents.

If the capability is fixed at build time and typed, that is a **tool**, not a skill —
use `Skill(skill: "add-tool")` instead. If the work is a multi-stage pipeline with
typed handoffs, use `Skill(skill: "domain-model-subtask-pipeline")`.

Proceed immediately to Step 1.

## Step 1 — Add the Dependencies

The umbrella does not pull either of these. Both are on the **beta version line**.

Path: `build.gradle.kts`

```kotlin
implementation("ai.koog:skills:1.2.0-beta")       // discoverSkills, generateSkillsPrompt
implementation("ai.koog:agents-ext:1.2.0-beta")   // ReadFileTool, ListDirectoryTool
```

`agents-ext` is required, not optional: without file tools the agent can see the
catalog but cannot read any skill body.

Proceed immediately to Step 2.

## Step 2 — Author the SKILL.md

Discovery expects **one directory per skill**, each containing a `SKILL.md` whose
YAML frontmatter carries `name` and `description`.

Path: `skills/<skill-name>/SKILL.md`

```markdown
---
name: <skill-name>
description: What this does and when to use it. The model sees this in the catalog and picks on it, so write a trigger, not a title.
---

# <skill-name>

Instructions, rules, vocabulary, examples — whatever the model needs to apply it.
```

Four hard requirements, each of which silently drops the skill when violated:

- `name` **must equal the parent directory name**
- both `name` and `description` must be present and non-blank
- malformed frontmatter is ignored with a warning, not an error
- duplicate names across roots collide — the precedence rule decides, so keep names unique

Proceed immediately to Step 3.

## Step 3 — Discover and Generate the Catalog

Path: `Main.kt`

```kotlin
import ai.koog.rag.base.files.JVMFileSystemProvider
import ai.koog.skills.discovery.discoverSkills
import ai.koog.skills.prompt.SkillsPromptFormat
import ai.koog.skills.prompt.generateSkillsPrompt

val skillsRoot = "/absolute/path/to/skills"
val discovered = discoverSkills(JVMFileSystemProvider.ReadOnly, listOf(skillsRoot))
val skillsPrompt = generateSkillsPrompt(discovered, SkillsPromptFormat.XML)
```

Use `JVMFileSystemProvider.ReadOnly` — a skills directory is input, and a read-only
provider means a prompt-injected instruction inside a SKILL.md cannot rewrite it.

Pass **absolute** paths. Under a Gradle `run` task the working directory is the
module directory, not the project root, so a relative root silently discovers nothing.
Wire it through explicitly:

```kotlin
// build.gradle.kts
tasks.named<JavaExec>("run") {
    systemProperty("skills.root", rootProject.layout.projectDirectory.dir("skills").asFile.absolutePath)
}
```

Proceed immediately to Step 4.

## Step 4 — Wire the Agent

Path: `Main.kt`

```kotlin
val agent = AIAgent(
    promptExecutor = executor,
    systemPrompt = """
        Before using a skill, disclose it: list the skill directory and read the
        SKILL.md, then apply it.

        $skillsPrompt
    """.trimIndent(),
    llmModel = model,
    toolRegistry = ToolRegistry {
        tool(ListDirectoryTool(JVMFileSystemProvider.ReadOnly))
        tool(ReadFileTool(JVMFileSystemProvider.ReadOnly))
    },
)
```

**Instruct the agent to disclose before applying.** Two reasons: the tool trace becomes
a readable audit of which skill fired, and a skill that was silently mis-selected is
otherwise invisible. `SkillsPromptFormat` also offers `YAML` and `JSON`; XML is the
default choice because the catalog nests and XML degrades most gracefully when a
description contains markup.

Proceed immediately to Step 5.

## Step 5 — Verify

Confirm all four, in order:

1. Discovery is non-empty: `println(discovered.joinToString { it.name })`. Empty means
   a relative path, a `name`/directory mismatch, or malformed frontmatter — check the
   warning log before touching anything else
2. The agent's tool trace shows a directory listing **and** a file read before it acts
3. Adding a new `SKILL.md` and re-running picks it up with **no recompile**. If it does
   not, the root is wrong
4. The output actually reflects the skill body, not just its description

## Security

Skill bodies are instructions the model will follow, and they come from the
filesystem. Treat a skills root exactly like any other untrusted input:

- never point discovery at a directory writable by someone you would not let edit the
  system prompt
- keep the file provider read-only
- a skill cannot be trusted to constrain itself — enforce real limits with the tool
  registry, which the skill body cannot change
