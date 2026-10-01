# Confined Skill File Tools

Register `ConfinedSkillFiles(skillsRoot)` as a `ToolSet` when reads must stay under
one trusted root. Paths are absolute; traversal and symlink escapes are rejected.

<!-- compile-example: ConfinedFiles -->
```kotlin
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import java.nio.file.Files
import java.nio.file.Path

class ConfinedSkillFiles(root: Path) : ToolSet {
    private val allowedRoot = root.toRealPath()

    private fun checkedPath(path: String): Path {
        val requested = Path.of(path)
        require(requested.isAbsolute) { "Use an absolute skill path" }
        val canonical = requested.toRealPath()
        require(canonical.startsWith(allowedRoot)) { "Choose an existing path beneath the allowed skills root" }
        return canonical
    }

    @Tool
    fun readSkill(path: String): String = Files.readString(checkedPath(path))

    @Tool
    fun listSkills(path: String): List<String> = Files.list(checkedPath(path)).use { entries ->
        entries.map { checkedPath(it.toString()).toString() }.sorted().toList()
    }
}
```

The root must exist before construction. Use this tool set for reads and listings;
do not also register an unrestricted filesystem reader. Keep discovered paths and
this tool set's root aligned.

Canonicalization checks a path before opening it. A concurrent filesystem writer
can race that check. Use an OS sandbox or a separate process with restricted mounts
for attacker-controlled files; this helper is for a trusted, stable filesystem.
