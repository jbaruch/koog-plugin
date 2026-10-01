# Stream the Agent's Reply to the Caller

## Problem/Feature Description

A developer is building an interactive chat interface backed by a Koog 1.3 agent. They want the LLM's reply to stream — partial tokens appear in the UI as they're generated, not in one final blob at the end. The agent's reply is text-only (no tools); the user just types something and the agent responds.

They want to author a custom strategy because the default `singleRunStrategy()` doesn't expose the streaming variant.

## Output Specification

Walk through the strategy and how the chunks are surfaced. Produce the strategy source as a single Kotlin snippet, labeled.
