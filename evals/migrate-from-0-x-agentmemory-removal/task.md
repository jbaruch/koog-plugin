# Bump a 0.x Codebase Using AgentMemory to 1.3

## Problem/Feature Description

A developer maintains a Koog 0.7.x agent that uses the memory feature. Their current code includes:

```kotlin
val agent = AIAgent.invoke(
    promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    llmModel = OpenAIModels.Chat.GPT4o,
    systemPrompt = "...",
) {
    install(AgentMemory) {
        queryExtractor = MyQueryExtractor()
        extractionStrategy = MyExtractionStrategy()
        ingestionTiming = IngestionTiming.AFTER_RESPONSE
    }
}
```

They want to bump to Koog 1.3. They're targeting JDK 11 today.

## Output Specification

Walk through all the changes required to bring this snippet to 1.3. Produce the changed Kotlin code and the dependency/toolchain changes as a single response, labeled.
