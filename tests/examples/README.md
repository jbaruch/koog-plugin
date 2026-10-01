# Published example checks

Run `gradle -p tests/examples ktlintCheck` before `gradle -p tests/examples check`
with JDK 21 and Gradle 9.2.1. CI pins the Temurin build.
The CI job installs both tools. Generated sources and compiled artifacts stay in `build/`.

`extractExamples` reads the `compile-example` markers in the published skills.
It compiles the code inside each marked Kotlin fence without API substitutions.
Checks cover typed storage, long-term memory configuration, a PostgreSQL chat
provider, OpenTelemetry installation and JVM configuration, and OpenAI/Google
scaffolds. Compiler warnings fail the check.
Prompt construction, few-shot wiring, runtime context and prepared JDBC lookup
and functional-agent examples are compiled through the same fences.

Runtime tests exercise shared and isolated graph histories, storage checkpoint
round trips, discovery snapshots and rediscovery, arbitrary absolute
paths accepted by the default read-only reader, and synthetic inherited billing
variables passed through the CLI process transport. No live model, telemetry,
database, vendor CLI, or private-file access is required.
Prepared-query tests use a temporary local H2 database to verify interval filtering,
result caps and invalid-input rejection without production credentials.

The confined-reader tests cover in-root reads/listings, traversal, sibling-prefix
paths, and symlink escapes on a stable filesystem. Concurrent filesystem races
require OS-level isolation and are outside the helper's contract.

Renew Koog pins after each upstream release. Review Kotlin, Gradle, PostgreSQL,
and JUnit pins quarterly against their official releases. Review the pinned Temurin
security build monthly. ktlint follows the formatter used by the tagged Koog source.
Regenerate dependency
locks with `gradle -p tests/examples check --write-locks` after reviewing a bump.
