# Migrate a Custom-Strategy Agent With a Shared HTTP Client to 1.3

## Problem/Feature Description

A developer maintains a Koog 0.7.x agent that uses a hand-authored graph strategy with a tool loop, and constructs its LLM client by hand so it can share one tuned HTTP client (timeouts, corporate proxy) across the whole app. They also inject a clock so the run timestamps are testable.

`build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.0.21"
    application
}

dependencies {
    implementation("ai.koog:koog-agents:0.7.1")
    implementation("io.ktor:ktor-client-cio:2.3.12")
}

kotlin {
    jvmToolchain(11)
}
```

`Strategy.kt`:

```kotlin
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*

val weatherChat = strategy<String, String>("weather-chat") {
    val callModel by nodeLLMRequest()
    val runTools by nodeExecuteTools()   // tool results are written back to the prompt automatically

    edge(nodeStart forwardTo callModel)
    edge(callModel forwardTo runTools onToolCall { true })
    edge(callModel forwardTo nodeFinish onAssistantMessage { true })
    edge(runTools forwardTo callModel)   // loop back to the model after the tools have run
}
```

`Main.kt`:

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

fun main() = runBlocking {
    val sharedHttp = HttpClient(CIO) {
        // tuned timeouts + corporate proxy shared across the app
    }

    val client = OpenAILLMClient(System.getenv("OPENAI_API_KEY"), httpClient = sharedHttp)
    val executor = SingleLLMPromptExecutor(client)

    val agent = AIAgent.invoke(
        promptExecutor = executor,
        strategy = weatherChat,
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You are a weather assistant.",
        toolRegistry = weatherTools,
        clock = Clock.System,
    )

    println(agent.run("What's the weather in Paris?"))
}
```

They want to bring this project to Koog 1.3 with the same behavior — keep the shared, tuned HTTP client and the injectable clock. They're on JDK 11 today.

## Output Specification

Walk through every change required to compile and run this against Koog 1.3. Produce the changed `build.gradle.kts`, `Strategy.kt`, and `Main.kt` as a single response, each file clearly labeled.
