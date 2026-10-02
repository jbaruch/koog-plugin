import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("ai.koog:koog-agents:1.3.0")
    implementation("ai.koog:agents-features-longterm-memory:1.3.0-beta")
    implementation("ai.koog:agents-features-chat-history-jdbc:1.3.0")
    implementation("ai.koog:agents-features-persistence-jdbc:1.3.0")
    implementation("ai.koog:agents-features-opentelemetry:1.3.0")
    implementation("ai.koog:prompt-executor-google-client:1.3.0-beta")
    implementation("ai.koog:prompt-executor-llms-all:1.3.0-beta")
    implementation("ai.koog:skills:1.3.0-beta")
    implementation("ai.koog:agents-ext:1.3.0-beta")
    implementation("ai.koog:agents-cli:1.3.0-beta")
    implementation("org.postgresql:postgresql:42.7.10")
    testImplementation("ai.koog:agents-test:1.3.0")
    testImplementation("com.h2database:h2:2.4.240")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.8.2")
}

dependencyLocking {
    lockAllConfigurations()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

ktlint {
    filter {
        exclude {
            it.file.toPath().startsWith(
                layout.buildDirectory
                    .get()
                    .asFile
                    .toPath(),
            )
        }
    }
}

val examples =
    mapOf(
        "Storage" to "manage-state/SKILL.md",
        "LongMemory" to "manage-state/SKILL.md",
        "ChatHistory" to "persist-chat-history/SKILL.md",
        "Telemetry" to "add-observability/SKILL.md",
        "TelemetryConfig" to "add-observability/SKILL.md",
        "Scaffold" to "scaffold-agent/SKILL.md",
        "GoogleScaffold" to "scaffold-agent/SKILL.md",
        "ConfinedFiles" to "use-agent-skills/references/confined-file-tools.md",
        "SimplePrompt" to "define-prompt/SKILL.md",
        "FewShotPrompt" to "define-prompt/SKILL.md",
        "RuntimePrompt" to "define-prompt/SKILL.md",
        "SqlLookup" to "query-sql-from-agent/SKILL.md",
        "FunctionalAgent" to "use-functional-agent/SKILL.md",
        "CheckpointReplay" to "add-persistence/SKILL.md",
        "JdbcCheckpointAgent" to "add-persistence/references/durable-agents.md",
        "FileCheckpointAgent" to "add-persistence/references/durable-agents.md",
        "CheckpointFork" to "snapshot-and-restore/references/checkpoint-fork.md",
    )
val generatedExamples = layout.buildDirectory.dir("generated/examples")
val extractExamples by tasks.registering {
    inputs.files(examples.values.map { file("../../skills/$it") })
    outputs.dir(generatedExamples)
    doLast {
        val directory = generatedExamples.get().asFile
        check(!directory.exists() || directory.deleteRecursively()) {
            "Cannot clear $directory; fix its permissions before extracting examples"
        }
        check(directory.mkdirs() || directory.isDirectory) {
            "Cannot create $directory; fix its parent directory permissions"
        }
        for ((name, path) in examples) {
            val markdown = file("../../skills/$path").readText()
            val marker = "<!-- compile-example: $name -->\n```kotlin\n"
            val start = markdown.indexOf(marker)
            check(start >= 0 && markdown.indexOf(marker, start + 1) < 0) {
                "Expected one $name example in skills/$path"
            }
            val bodyStart = start + marker.length
            val end = markdown.indexOf("\n```", bodyStart)
            check(end >= 0) { "Close the $name Kotlin fence in skills/$path" }
            val body = markdown.substring(bodyStart, end)
            val source = if (body.startsWith("package ")) body else "package verified\n\n$body"
            directory.resolve("$name.kt").writeText("$source\n")
        }
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(generatedExamples)
}
tasks.named("compileKotlin") {
    dependsOn(extractExamples)
}
tasks.matching { it.name.startsWith("runKtlint") && it.name.endsWith("OverMainSourceSet") }.configureEach {
    dependsOn(extractExamples)
}
tasks.test {
    useJUnitPlatform()
    // Probe JVMs receive synthetic credentials, never the developer's environment.
    systemProperty(
        "probe.classpath",
        sourceSets.test
            .get()
            .runtimeClasspath.asPath,
    )
}
