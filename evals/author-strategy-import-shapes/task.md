# Add a Tool-Handling Loop to a Koog Strategy

## Problem/Feature Description

A developer is writing a Koog 1.3 strategy by hand (not using `singleRunStrategy()`). The agent needs to: take a user message, call the LLM, and from there one of two things happens — either the LLM produced text, in which case the agent finishes and returns the text; or the LLM tried to call tools, in which case those tools execute and their results go back to the LLM, looping until it produces text. Standard tool-handling shape.

They have a scaffolded `main()` that constructs an `AIAgent`, and they're about to write a custom strategy file. They mention that they've heard `singleRunStrategy()` already handles this loop internally, but they want to author it themselves so they can later branch on intermediate state — they're not just looking for the default behavior.

## Output Specification

Produce the strategy file as Kotlin source, including the `import` block at the top.
