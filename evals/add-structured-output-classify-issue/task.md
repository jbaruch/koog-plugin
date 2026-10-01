# Make an Agent Return a Typed Classification

## Problem/Feature Description

A developer has a working Koog 1.3 agent that classifies GitHub issues — currently it returns a free-form string and downstream code parses it with regex (brittle). They want the agent to return a typed object directly:

```kotlin
@Serializable
data class IssueClassification(
    val type: String,        // "bug" | "feature" | "question"
    val confidence: Double,
    val suggestedLabels: List<String>,
)
```

The agent currently uses the default strategy. They want the smallest change that gets them the typed result.

## Output Specification

Update the project so `agent.run(...)` returns an `IssueClassification` directly instead of a free-form string the caller has to parse.
