# Add an MCP Server's Tools Alongside Existing Local Tools

## Problem/Feature Description

A developer has a Koog 1.3 agent that already has its own tools registered — they wrote a `CalculatorTools` ToolSet class and they register it as:

```kotlin
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = ToolRegistry { tools(CalculatorTools().asTools()) },
    systemPrompt = "You are a helpful assistant.",
)
```

They want to keep the calculator tools and *also* connect to a remote MCP server at `https://mcp.weather.example/v1` (which speaks the modern HTTP-based MCP transport). They don't want to drop the calculator tools; they want the LLM to choose between the calculator tools and the weather server's tools depending on what the user asks.

## Output Specification

Walk through how to add the MCP server while preserving the existing CalculatorTools. Produce the modified source in a single response.
